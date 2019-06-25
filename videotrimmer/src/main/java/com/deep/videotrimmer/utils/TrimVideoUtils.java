/*
 * MIT License
 *
 * Copyright (c) 2016 Knowledge, education for life.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.deep.videotrimmer.utils;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.LoggingCore;
import com.coremedia.iso.boxes.Container;
import com.deep.videotrimmer.interfaces.OnTrimVideoListener;
import com.googlecode.mp4parser.FileDataSourceViaHeapImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.builder.Mp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
/**
 * Created by Deep Patel
 * (Sr. Android Developer)
 * on 6/4/2018
 */
public class TrimVideoUtils {

    private static final String TAG = TrimVideoUtils.class.getSimpleName();
	
	/**
	 * Start a video trim Asynchronously
	 * @param src
	 * @param dst
	 * @param startMs
	 * @param endMs
	 * @param userDefinedFileOutputLoc
	 * @param callback
	 * @throws IOException
	 */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void startTrim(@NonNull File src,
                                 @NonNull String dst,
                                 long startMs, long endMs,
                                 boolean userDefinedFileOutputLoc,
                                 @NonNull OnTrimVideoListener callback) throws IOException {
        String filePath = null;
        if(userDefinedFileOutputLoc){
            filePath = dst;
        } else {
            final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            final String fileName = "MP4_" + timeStamp + ".mp4";
            filePath = dst + fileName;
        }
        File file = new File(filePath);
        try {
            //Create the parent folder if it does not exist
            file.getParentFile().mkdirs();
        } catch (Exception e){
            e.printStackTrace();
        }
        Log.d(TAG, "Generated file path " + filePath);
        genVideoUsingMp4Parser(src, file, startMs, endMs, callback);
    }
	
	/**
	 * Start a video trim Synchronously.
	 * Note, MAKE SURE THIS IS RUNNING ON A BACKGROUND THREAD! If not, you will cause ANR errors.
	 * This option is available in the code because some people may want to run this in their own
	 * asynchronous logic instead of the build-in one to this class.
	 * @param src
	 * @param dst
	 * @param startMs
	 * @param endMs
	 * @param userDefinedFileOutputLoc
	 * @param callback
	 * @return
	 * @throws IOException
	 */
	public static Uri startTrimSynchronous(@NonNull File src,
	                             @NonNull String dst,
	                             long startMs, long endMs,
	                             boolean userDefinedFileOutputLoc,
	                             @NonNull OnTrimVideoListener callback) throws IOException {
		String filePath = null;
		if(userDefinedFileOutputLoc){
			filePath = dst;
		} else {
			final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
			final String fileName = "MP4_" + timeStamp + ".mp4";
			filePath = dst + fileName;
		}
		File file = new File(filePath);
		try {
			//Create the parent folder if it does not exist
			file.getParentFile().mkdirs();
		} catch (Exception e){
			e.printStackTrace();
		}
		Log.d(TAG, "Generated file path " + filePath);
		return genVideoUsingMp4ParserSynchronous(src, file, startMs, endMs, callback);
	}
	
	/**
	 * Start a video trim Synchronously.
	 * Note, MAKE SURE THIS IS RUNNING ON A BACKGROUND THREAD! If not, you will cause ANR errors.
	 * This option is available in the code because some people may want to run this in their own
	 * asynchronous logic instead of the build-in one to this class.
	 * @param src
	 * @param dst
	 * @param startMs
	 * @param endMs
	 * @param callback
	 * @return
	 * @throws IOException
	 */
	private static Uri genVideoUsingMp4ParserSynchronous(@NonNull File src, @NonNull File dst, long startMs, long endMs, @Nullable OnTrimVideoListener callback) throws IOException {
		
		Movie movie = null;
		try {
			movie = MovieCreator.build(new FileDataSourceViaHeapImpl(src.getAbsolutePath()));
		} catch (Exception e){
			if(callback != null) {
				callback.invalidVideo();
			}
			e.printStackTrace();
			return null;
		}
		
		List<Track> tracks = movie.getTracks();
		movie.setTracks(new LinkedList<Track>());
		
		double startTime1 = startMs / 1000;
		double endTime1 = endMs / 1000;
		
		boolean timeCorrected = false;
		
		for (Track track : tracks) {
			if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
				if (timeCorrected) {
                   /*  This exception here could be a false positive in case we have multiple tracks
                     with sync samples at exactly the same positions. E.g. a single movie containing
                     multiple qualities of the same video (Microsoft Smooth Streaming file)*/
					
					throw new RuntimeException("The startTime has already been corrected by another track with SyncSample. Not Supported.");
				}
				startTime1 = correctTimeToSyncSample(track, startTime1, false);
				endTime1 = correctTimeToSyncSample(track, endTime1, true);
				timeCorrected = true;
			}
		}
		
		for (Track track : tracks) {
			long currentSample = 0;
			double currentTime = 0;
			double lastTime = -1;
			long startSample1 = -1;
			long endSample1 = -1;
			
			for (int i = 0; i < track.getSampleDurations().length; i++) {
				long delta = track.getSampleDurations()[i];
				
				
				if (currentTime > lastTime && currentTime <= startTime1) {
					/*current sample is still before the new starttime*/
					startSample1 = currentSample;
				}
				if (currentTime > lastTime && currentTime <= endTime1) {
					/* current sample is after the new start time and still before the new endtime*/
					endSample1 = currentSample;
				}
				lastTime = currentTime;
				currentTime += (double) delta / (double) track.getTrackMetaData().getTimescale();
				currentSample++;
			}
			movie.addTrack(new AppendTrack(new CroppedTrack(track, startSample1, endSample1)));
		}
		
		dst.getParentFile().mkdirs();
		
		if (!dst.exists()) {
			try {
				dst.createNewFile();
			} catch (Exception e){
				e.printStackTrace();
				if(callback != null) {
					callback.invalidVideo();
				}
				return null;
			}
		}
		LoggingCore.setShouldLog(true);
		Container out = new DefaultMp4Builder().build(movie, new DefaultMp4Builder.Mp4TrimmerTimeCallback() {
			@Override
			public void chunkWritten(long l, long l1, float v) {
			
			}
		});
		
		FileOutputStream fos = new FileOutputStream(dst);
		FileChannel fc = fos.getChannel();
		out.writeContainer(fc);
		
		fc.close();
		fos.close();
		return Uri.parse(dst.toString());
	}
	
	/**
	 * Start a video trim Asynchronously
	 * @param src
	 * @param dst
	 * @param startMs
	 * @param endMs
	 * @param callback
	 * @throws IOException
	 */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void genVideoUsingMp4Parser(@NonNull File src, @NonNull File dst, long startMs, long endMs, @NonNull OnTrimVideoListener callback) throws IOException {
		Uri uri = genVideoUsingMp4ParserSynchronous(src, dst, startMs, endMs, callback);
		if(uri != null){
			callback.getResult(uri);
		}
    }

    private static double correctTimeToSyncSample(@NonNull Track track, double cutHere, boolean next) {
        double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
        long currentSample = 0;
        double currentTime = 0;
        for (int i = 0; i < track.getSampleDurations().length; i++) {
            long delta = track.getSampleDurations()[i];

            if (Arrays.binarySearch(track.getSyncSamples(), currentSample + 1) >= 0) {
                timeOfSyncSamples[Arrays.binarySearch(track.getSyncSamples(), currentSample + 1)] = currentTime;
            }
            currentTime += (double) delta / (double) track.getTrackMetaData().getTimescale();
            currentSample++;

        }
        double previous = 0;
        for (double timeOfSyncSample : timeOfSyncSamples) {
            if (timeOfSyncSample > cutHere) {
                if (next) {
                    return timeOfSyncSample;
                } else {
                    return previous;
                }
            }
            previous = timeOfSyncSample;
        }
        return timeOfSyncSamples[timeOfSyncSamples.length - 1];
    }
}
