package com.deep.videotrimmer;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.deep.videotrimmer.interfaces.BackgroundErrorListener;
import com.deep.videotrimmer.interfaces.OnTrimVideoListener;
import com.deep.videotrimmer.utils.BackgroundExecutor;
import com.deep.videotrimmer.utils.TrimVideoUtils;
import com.deep.videotrimmer.utils.UiThreadExecutor;

import java.io.File;

/**
 * This class is used as a standalone trimmer to use in case someone wants to trim a video
 * without providing a UI to do so
 */
public class StandaloneVideoTrimmer {
	
	private static final String NEED_END_TIME_OR_CONTEXT =
			"If your 'endTimeInMilliseconds' is null you must include a 'context' variable as " +
					"well. You are receiving this error because both values are null or invalid";
	
	//region Public Methods for Trimming Video - Asynchronous
	
	/**
	 * Trim a video file to the specified length and width
	 * @param context
	 * @param trimVideoListener
	 * @param videoToTrim
	 * @param destinationFilePath
	 * @param startTimeInMilliseconds the start time to begin the video trim at in milliseconds
	 * @param endTimeInMilliseconds the end time to trim the video to in milliseconds
	 * @throws VideoTrimmingException Will throw this exception if both the context and
	 *         endTimeInMilliseconds variables are null. At least one must not be null
	 */
	public static void trimVideo(@Nullable Context context,
	                             @NonNull OnTrimVideoListener trimVideoListener,
	                             @NonNull File videoToTrim,
	                             @NonNull String destinationFilePath,
	                             @Nullable Integer startTimeInMilliseconds,
	                             @Nullable Integer endTimeInMilliseconds) throws VideoTrimmingException {
		StandaloneVideoTrimmer.checkForValidParams(context, endTimeInMilliseconds);
		StandaloneVideoTrimmer.startTrimVideo(videoToTrim, destinationFilePath,
				(isValidMillisecondsTime(startTimeInMilliseconds)) ? startTimeInMilliseconds : 0,
				(isValidMillisecondsTime(endTimeInMilliseconds)) ? endTimeInMilliseconds : ((int)getVideoLength(videoToTrim, context)),
				true, trimVideoListener);
	}
	
	/**
	 * Trim a video file to the specified length and width
	 * @param context
	 * @param trimVideoListener
	 * @param videoToTrim
	 * @param destinationFilePath
	 * @param startTimeInMilliseconds the start time to begin the video trim at in milliseconds
	 * @param endTimeInMilliseconds the end time to trim the video to in milliseconds
	 * @throws VideoTrimmingException Will throw this exception if both the context and
	 *         endTimeInMilliseconds variables are null. At least one must not be null
	 */
	public static void trimVideo(@Nullable Context context,
	                             @NonNull OnTrimVideoListener trimVideoListener,
	                             @NonNull Uri videoToTrim,
	                             @NonNull String destinationFilePath,
	                             @Nullable Integer startTimeInMilliseconds,
	                             @Nullable Integer endTimeInMilliseconds) throws VideoTrimmingException {
		File file = new File(videoToTrim.getPath());
		StandaloneVideoTrimmer.checkForValidParams(context, endTimeInMilliseconds);
		StandaloneVideoTrimmer.startTrimVideo(file, destinationFilePath,
				(isValidMillisecondsTime(startTimeInMilliseconds)) ? startTimeInMilliseconds : 0,
				(isValidMillisecondsTime(endTimeInMilliseconds)) ? endTimeInMilliseconds : ((int)getVideoLength(file, context)),
				true, trimVideoListener);
	}
	
	/**
	 * Trim a video to the specified length and width
	 * @param context
	 * @param trimVideoListener
	 * @param videoToTrim
	 * @param startTimeInMilliseconds
	 * @param endTimeInMilliseconds
	 * @throws VideoTrimmingException
	 */
	public static void trimVideo(@Nullable Context context,
	                             @NonNull OnTrimVideoListener trimVideoListener,
	                             @NonNull File videoToTrim,
	                             @Nullable Integer startTimeInMilliseconds,
	                             @Nullable Integer endTimeInMilliseconds) throws VideoTrimmingException {
		File folder = Environment.getExternalStorageDirectory();
		String destinationFilePath = folder.getPath() + File.separator;
		StandaloneVideoTrimmer.checkForValidParams(context, endTimeInMilliseconds);
		StandaloneVideoTrimmer.startTrimVideo(videoToTrim, destinationFilePath,
				(isValidMillisecondsTime(startTimeInMilliseconds)) ? startTimeInMilliseconds : 0,
				(isValidMillisecondsTime(endTimeInMilliseconds)) ? endTimeInMilliseconds : ((int)getVideoLength(videoToTrim, context)),
				true, trimVideoListener);
	}
	
	/**
	 * Trim a video to the specified length and width
	 * @param context
	 * @param trimVideoListener
	 * @param videoToTrim
	 * @param startTimeInMilliseconds
	 * @param endTimeInMilliseconds
	 * @throws VideoTrimmingException
	 */
	public static void trimVideo(@Nullable Context context,
	                             @NonNull OnTrimVideoListener trimVideoListener,
	                             @NonNull Uri videoToTrim,
	                             @Nullable Integer startTimeInMilliseconds,
	                             @Nullable Integer endTimeInMilliseconds) throws VideoTrimmingException {
		File folder = Environment.getExternalStorageDirectory();
		String destinationFilePath = folder.getPath() + File.separator;
		File file = new File(videoToTrim.getPath());
		StandaloneVideoTrimmer.checkForValidParams(context, endTimeInMilliseconds);
		StandaloneVideoTrimmer.startTrimVideo(file, destinationFilePath,
				(isValidMillisecondsTime(startTimeInMilliseconds)) ? startTimeInMilliseconds : 0,
				(isValidMillisecondsTime(endTimeInMilliseconds)) ? endTimeInMilliseconds : ((int)getVideoLength(file, context)),
				true, trimVideoListener);
	}
	
	//endregion
	
	//region Public Methods for Trimming Video - Synchronous
	
	/**
	 * Trim a video file synchronously to the specified length and width
	 * Note, MAKE SURE THIS IS RUNNING ON A BACKGROUND THREAD! If not, you will cause ANR errors.
	 * This option is available in the code because some people may want to run this in their own
	 * asynchronous logic instead of the build-in one to this class.
	 * @param context
	 * @param videoToTrim
	 * @param destinationFilePath
	 * @param startTimeInMilliseconds the start time to begin the video trim at in milliseconds
	 * @param endTimeInMilliseconds the end time to trim the video to in milliseconds
	 * @throws VideoTrimmingException Will throw this exception if both the context and
	 *         endTimeInMilliseconds variables are null. At least one must not be null
	 */
	public static Uri trimVideoSynchronous(@Nullable Context context,
	                             @NonNull File videoToTrim,
	                             @NonNull String destinationFilePath,
	                             @Nullable Integer startTimeInMilliseconds,
	                             @Nullable Integer endTimeInMilliseconds) throws VideoTrimmingException {
		StandaloneVideoTrimmer.checkForValidParams(context, endTimeInMilliseconds);
		return StandaloneVideoTrimmer.startTrimVideoSynchronously(videoToTrim, destinationFilePath,
				(isValidMillisecondsTime(startTimeInMilliseconds)) ? startTimeInMilliseconds : 0,
				(isValidMillisecondsTime(endTimeInMilliseconds)) ? endTimeInMilliseconds : ((int)getVideoLength(videoToTrim, context)),
				true);
	}
	
	/**
	 * Trim a video file synchronously to the specified length and width
	 * Note, MAKE SURE THIS IS RUNNING ON A BACKGROUND THREAD! If not, you will cause ANR errors.
	 * This option is available in the code because some people may want to run this in their own
	 * asynchronous logic instead of the build-in one to this class.
	 * @param context
	 * @param videoToTrim
	 * @param destinationFilePath
	 * @param startTimeInMilliseconds the start time to begin the video trim at in milliseconds
	 * @param endTimeInMilliseconds the end time to trim the video to in milliseconds
	 * @throws VideoTrimmingException Will throw this exception if both the context and
	 *         endTimeInMilliseconds variables are null. At least one must not be null
	 */
	public static Uri trimVideoSynchronous(@Nullable Context context,
	                             @NonNull Uri videoToTrim,
	                             @NonNull String destinationFilePath,
	                             @Nullable Integer startTimeInMilliseconds,
	                             @Nullable Integer endTimeInMilliseconds) throws VideoTrimmingException {
		File file = new File(videoToTrim.getPath());
		StandaloneVideoTrimmer.checkForValidParams(context, endTimeInMilliseconds);
		return StandaloneVideoTrimmer.startTrimVideoSynchronously(file, destinationFilePath,
				(isValidMillisecondsTime(startTimeInMilliseconds)) ? startTimeInMilliseconds : 0,
				(isValidMillisecondsTime(endTimeInMilliseconds)) ? endTimeInMilliseconds : ((int)getVideoLength(file, context)),
				true);
	}
	
	/**
	 * Trim a video synchronously to the specified length and width
	 * Note, MAKE SURE THIS IS RUNNING ON A BACKGROUND THREAD! If not, you will cause ANR errors.
	 * This option is available in the code because some people may want to run this in their own
	 * asynchronous logic instead of the build-in one to this class.
	 * @param context
	 * @param videoToTrim
	 * @param startTimeInMilliseconds
	 * @param endTimeInMilliseconds
	 * @throws VideoTrimmingException
	 */
	public static Uri trimVideoSynchronous(@Nullable Context context,
	                             @NonNull File videoToTrim,
	                             @Nullable Integer startTimeInMilliseconds,
	                             @Nullable Integer endTimeInMilliseconds) throws VideoTrimmingException {
		File folder = Environment.getExternalStorageDirectory();
		String destinationFilePath = folder.getPath() + File.separator;
		StandaloneVideoTrimmer.checkForValidParams(context, endTimeInMilliseconds);
		return StandaloneVideoTrimmer.startTrimVideoSynchronously(videoToTrim, destinationFilePath,
				(isValidMillisecondsTime(startTimeInMilliseconds)) ? startTimeInMilliseconds : 0,
				(isValidMillisecondsTime(endTimeInMilliseconds)) ? endTimeInMilliseconds : ((int)getVideoLength(videoToTrim, context)),
				true);
	}
	
	/**
	 * Trim a video synchronously to the specified length and width.
	 * Note, MAKE SURE THIS IS RUNNING ON A BACKGROUND THREAD! If not, you will cause ANR errors.
	 * This option is available in the code because some people may want to run this in their own
	 * asynchronous logic instead of the build-in one to this class.
	 *
	 * @param context
	 * @param videoToTrim
	 * @param startTimeInMilliseconds
	 * @param endTimeInMilliseconds
	 * @throws VideoTrimmingException
	 */
	public static Uri trimVideoSynchronous(@Nullable Context context,
	                             @NonNull Uri videoToTrim,
	                             @Nullable Integer startTimeInMilliseconds,
	                             @Nullable Integer endTimeInMilliseconds) throws VideoTrimmingException {
		File folder = Environment.getExternalStorageDirectory();
		String destinationFilePath = folder.getPath() + File.separator;
		File file = new File(videoToTrim.getPath());
		StandaloneVideoTrimmer.checkForValidParams(context, endTimeInMilliseconds);
		return StandaloneVideoTrimmer.startTrimVideoSynchronously(file, destinationFilePath,
				(isValidMillisecondsTime(startTimeInMilliseconds)) ? startTimeInMilliseconds : 0,
				(isValidMillisecondsTime(endTimeInMilliseconds)) ? endTimeInMilliseconds : ((int)getVideoLength(file, context)),
				true);
	}
	
	//endregion
	
	//region Utility Methods
	
	/**
	 * Get the length of the video file in milliseconds
	 * @param file
	 * @return The video length in milliseconds. Will return -1 if an error occurs
	 */
	public static long getVideoLength(@NonNull File file, @NonNull Context context){
		if(file == null){
			return -1;
		}
		MediaMetadataRetriever mdr = StandaloneVideoTrimmer.buildMediaDataReceiver(context, Uri.fromFile(file));
		String durationString = mdr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
		if(durationString == null){
			return -1;
		}
		try {
			return Long.parseLong(mdr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
		} catch (Exception e){
			return -1;
		}
	}
	
	/**
	 * Get the length of the video file in milliseconds
	 * @param mdr MediaMetadataRetriever with an already set Data Source
	 * @return The video length in milliseconds. Will return -1 if an error occurs
	 */
	public static long getVideoLength(@NonNull MediaMetadataRetriever mdr){
		String durationString = mdr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
		if(durationString == null){
			return -1;
		}
		try {
			return Long.parseLong(mdr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
		} catch (Exception e){
			return -1;
		}
	}
	
	/**
	 * Build, instantiate, and return a {@link MediaMetadataRetriever}
	 * @param context Context
	 * @param source Uri Source. If passing File, call {@link Uri#fromFile(File)}
	 * @return {@link MediaMetadataRetriever}
	 */
	public static MediaMetadataRetriever buildMediaDataReceiver(@NonNull Context context,
	                                                             @NonNull Uri source){
		MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
		mediaMetadataRetriever.setDataSource(context, source);
		return mediaMetadataRetriever;
	}
	
	/**
	 * Simple checker for the end time in milliseconds
	 * @param endTimeInMilliseconds
	 * @return
	 */
	private static boolean isValidMillisecondsTime(Integer endTimeInMilliseconds){
		if(endTimeInMilliseconds == null){
			return false;
		}
		if(endTimeInMilliseconds < 0){
			return false;
		}
		return true;
	}
	
	/**
	 * Checks for valid params; throws exception if invalid
	 * @param context
	 * @param endTimeInMilliseconds
	 * @throws VideoTrimmingException
	 */
	private static void checkForValidParams(Context context, Integer endTimeInMilliseconds) throws VideoTrimmingException{
		if (!isValidMillisecondsTime(endTimeInMilliseconds)) {
			if(context == null){
				throw new VideoTrimmingException(NEED_END_TIME_OR_CONTEXT);
			}
		}
	}
	
	//endregion
	
	//region Private Methods
	
	/**
	 * Start the video trimming process
	 * Note, this is an Asynchronous method and will pass data back along the
	 * passed {@link OnTrimVideoListener}
	 * @param file
	 * @param dst
	 * @param startVideo
	 * @param endVideo
	 * @param userDefinedCustomDest
	 * @param callback
	 */
	private static void startTrimVideo(@NonNull final File file, @NonNull final String dst,
	                            final int startVideo, final int endVideo,
	                            final boolean userDefinedCustomDest,
	                            @NonNull final OnTrimVideoListener callback) {
		final BackgroundErrorListener backgroundErrorListener = new BackgroundErrorListener() {
			@Override
			public void backgroundErrorTrigger(String error) {
				callback.invalidVideo();
			}
		};
		BackgroundExecutor.execute(
				new BackgroundExecutor.Task("", 0L, "") {
					@Override
					public void execute() {
						try {
							TrimVideoUtils.startTrim(file, dst, startVideo, endVideo, userDefinedCustomDest, callback);
						} catch (final Throwable e) {
							if (backgroundErrorListener != null) {
								UiThreadExecutor.runTask("",
										new Runnable() {
											@Override
											public void run() {
												backgroundErrorListener.backgroundErrorTrigger(e.getMessage());
											}
										}, 0L);
							} else {
								Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
							}
						}
					}
				}
		);
	}
	
	/**
	 * Start the video trimming process
	 * Note, this is a Synchronous method and will return a Uri
	 * @param file
	 * @param dst
	 * @param startVideo
	 * @param endVideo
	 * @param userDefinedCustomDest
	 * @return Uri of converted file. If it fails, will return null.
	 */
	private static Uri startTrimVideoSynchronously(@NonNull final File file, @NonNull final String dst,
	                                   final int startVideo, final int endVideo,
	                                   final boolean userDefinedCustomDest) {
		
		try {
			OnTrimVideoListener callback = new OnTrimVideoListener() {
				@Override
				public void invalidVideo() {
					Log.d("StandaloneVideoTrimmer", "Invalid Video passed as param");
				}
				
				@Override
				public void getResult(Uri uri) {
					//No logging needed her as it is returned automatically
				}
				
				@Override
				public void cancelAction() {
					Log.d("StandaloneVideoTrimmer", "Video Trimming Cancelled manually");
				}
			};
			return TrimVideoUtils.startTrimSynchronous(file, dst,
					startVideo, endVideo, userDefinedCustomDest, callback);
		} catch (final Throwable e) {
			e.printStackTrace();
			return null;
		}
	}
	
	//endregion
	
}
