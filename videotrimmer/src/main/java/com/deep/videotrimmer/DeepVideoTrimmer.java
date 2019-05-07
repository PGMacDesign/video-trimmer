package com.deep.videotrimmer;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.deep.videotrimmer.interfaces.OnProgressVideoListener;
import com.deep.videotrimmer.interfaces.OnRangeSeekBarListener;
import com.deep.videotrimmer.interfaces.OnTrimVideoListener;
import com.deep.videotrimmer.interfaces.ThumbnailGeneratingListener;
import com.deep.videotrimmer.utils.BackgroundExecutor;
import com.deep.videotrimmer.utils.ImageUtils;
import com.deep.videotrimmer.utils.TrimVideoUtils;
import com.deep.videotrimmer.utils.UiThreadExecutor;
import com.deep.videotrimmer.view.ProgressBarView;
import com.deep.videotrimmer.view.RangeSeekBarView;
import com.deep.videotrimmer.view.TimeLineView;
import com.googlecode.mp4parser.DataSource;
import com.googlecode.mp4parser.FileDataSourceViaHeapImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

public class DeepVideoTrimmer extends FrameLayout implements MediaPlayer.OnErrorListener,
		MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener,
		SeekBar.OnSeekBarChangeListener, OnRangeSeekBarListener, OnProgressVideoListener {
	
	private Context context;
	
	private static final String TAG = DeepVideoTrimmer.class.getSimpleName();
	private static final int MIN_TIME_FRAME = 1000;
	
	private SeekBar mHolderTopView;
	private RangeSeekBarView mRangeSeekBarView;
	private RelativeLayout mLinearVideo;
	private VideoView mVideoView;
	private ImageView mPlayView;
	private TextView mTextSize;
	private TextView mTextTimeFrame;
	private TextView mTextTime;
	private TimeLineView mTimeLineView;
	private ProgressBar timeLineProgressBar;
	private Button viewButtonCancel, viewButtonSave;
	
	private Uri mSrc;
	private String mFinalPath;
	private boolean userSetCustomOutput;
	
	private int mMaxDuration;
	private List<OnProgressVideoListener> mListeners;
	private OnTrimVideoListener mOnTrimVideoListener;
	
	private long minimumViableVideoSizeInKb = 250;
	private int mDuration = 0;
	private int maxFileSize = 25;
	private int mTimeVideo = 0;
	private int mStartPosition = 0;
	private int mEndPosition = 0;
	private long mOriginSizeFile;
	private long mFileSizeInKB = 0;
	private boolean mResetSeekBar = true;
	//region Progress Bar Vars
	
	/**
	 * Listener used for thumbnail generation indication to the user to let them know how
	 * many are left to be created. Link: {@link ThumbnailGeneratingListener}
	 */
	private ThumbnailGeneratingListener thumbnailListener;
	private boolean showProgressBarWhileLoadingBitmaps = true;
	private boolean shouldProgressBarBeIndeterminate = false;
	private long animationTimeForThumbnailProgressBar;
	private int progressBarColor;
	//endregion
	
	@NonNull
	private final MessageHandler mMessageHandler = new MessageHandler(this);
	private static final int SHOW_PROGRESS = 2;
	private boolean letUserProceed;
	private GestureDetector mGestureDetector;
	private int initialLength;
	@NonNull
	private final GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {
		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			if (mVideoView.isPlaying()) {
				mPlayView.setVisibility(View.VISIBLE);
				mMessageHandler.removeMessages(SHOW_PROGRESS);
				mVideoView.pause();
			} else {
				mPlayView.setVisibility(View.GONE);
				
				if (mResetSeekBar) {
					mResetSeekBar = false;
					mVideoView.seekTo(mStartPosition);
				}
				
				mMessageHandler.sendEmptyMessage(SHOW_PROGRESS);
				mVideoView.start();
			}
			return true;
		}
	};
	
	@NonNull
	private final View.OnTouchListener mTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, @NonNull MotionEvent event) {
			mGestureDetector.onTouchEvent(event);
			return true;
		}
	};
	
	public DeepVideoTrimmer(@NonNull Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public DeepVideoTrimmer(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}
	
	private void init(Context context) {
		this.context = context;
		this.userSetCustomOutput = false;
		LayoutInflater.from(context).inflate(R.layout.view_time_line, this, true);
		this.thumbnailListener = new ThumbnailGeneratingListener() {
			@Override
			public void thumbnailGenerated(int whichThumbnail, int totalNumberOfThumbnails) {
				if(mTimeLineView == null){
					return;
				}
				if(timeLineProgressBar == null){
					mTimeLineView.setVisibility(VISIBLE);
					return;
				}
				if(!showProgressBarWhileLoadingBitmaps){
					timeLineProgressBar.setVisibility(GONE);
					mTimeLineView.setVisibility(VISIBLE);
					return;
				}
				if(whichThumbnail >= totalNumberOfThumbnails){
					timeLineProgressBar.setVisibility(GONE);
					mTimeLineView.setVisibility(VISIBLE);
				} else {
					if(DeepVideoTrimmer.this.shouldProgressBarBeIndeterminate){
						timeLineProgressBar.setIndeterminate(true);
					} else {
						mTimeLineView.setVisibility(GONE);
						timeLineProgressBar.setVisibility(VISIBLE);
						float x = (float) whichThumbnail;
						float y = (float) totalNumberOfThumbnails;
						if(y <= 0){
							y = x + 1;
						}
						float z = (100) * ((float)(x / y));
						try {
							ObjectAnimator objectAnimator = ObjectAnimator.ofInt(
									timeLineProgressBar, "progress", (int) z);
							objectAnimator.setDuration(animationTimeForThumbnailProgressBar);
							objectAnimator.start();
						} catch (Exception e){
							timeLineProgressBar.setProgress((int)z);
						}
					}
				}
			}
			
			/**
			 * If this triggers, problem with all of the thumbnails, trigger callback
			 */
			@Override
			public void couldNotGenerateThumbnails() {
				mOnTrimVideoListener.invalidVideo();
			}
		};
		this.mHolderTopView = findViewById(R.id.handlerTop);
		ProgressBarView progressVideoView = findViewById(R.id.timeVideoView);
		this.mRangeSeekBarView = findViewById(R.id.timeLineBar);
		this.mLinearVideo = findViewById(R.id.layout_surface_view);
		this.mVideoView = findViewById(R.id.video_loader);
		this.mPlayView = findViewById(R.id.icon_video_play);
		this.mTextSize = findViewById(R.id.textSize);
		this.mTextTimeFrame = findViewById(R.id.textTimeSelection);
		this.mTextTime = findViewById(R.id.textTime);
		this.mTimeLineView = findViewById(R.id.timeLineView);
		this.timeLineProgressBar = findViewById(R.id.timeLineProgressBar);
		viewButtonCancel = findViewById(R.id.btCancel);
		viewButtonSave = findViewById(R.id.btSave);
		
		if (viewButtonCancel != null) {
			viewButtonCancel.setOnClickListener(
					new OnClickListener() {
						@Override
						public void onClick(View view) {
							mOnTrimVideoListener.cancelAction();
						}
					}
			);
		}
		
		if (viewButtonSave != null) {
			viewButtonSave.setOnClickListener(
					new OnClickListener() {
						@Override
						public void onClick(View view) {
							
							if((getFileSizeInKB() < minimumViableVideoSizeInKb)){
								mOnTrimVideoListener.invalidVideo();
								try {
									Toast.makeText(getContext(), "Your video was invalid, please select a different video", Toast.LENGTH_SHORT).show();
								} catch (Exception e) {
									//May trigger if done on background thread
								}
								return;
							}
							if (letUserProceed) {
								if (mStartPosition <= 0 && mEndPosition >= mDuration) {
									mOnTrimVideoListener.getResult(mSrc);
								} else {
									mPlayView.setVisibility(View.VISIBLE);
									mVideoView.pause();
									
									MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
									mediaMetadataRetriever.setDataSource(getContext(), mSrc);
									long METADATA_KEY_DURATION = Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
									
									File file = new File(mSrc.getPath());
									
									if (mTimeVideo < MIN_TIME_FRAME) {
										
										if ((METADATA_KEY_DURATION - mEndPosition) > (MIN_TIME_FRAME - mTimeVideo)) {
											mEndPosition += (MIN_TIME_FRAME - mTimeVideo);
										} else if (mStartPosition > (MIN_TIME_FRAME - mTimeVideo)) {
											mStartPosition -= (MIN_TIME_FRAME - mTimeVideo);
										}
									}
									startTrimVideo(file, mFinalPath, mStartPosition, mEndPosition,
											userSetCustomOutput, mOnTrimVideoListener);
								}
							} else {
								try {
									Toast.makeText(getContext(), "Please trim your video less than " + maxFileSize + "MB " + " of size", Toast.LENGTH_SHORT).show();
								} catch (Exception e) {
									//May trigger if done on background thread
								}
							}
							
						}
					}
			);
		}
		
		this.mListeners = new ArrayList<>();
		this.mListeners.add(this);
		this.mListeners.add(progressVideoView);
		
		this.mHolderTopView.setMax(1000);
		this.mHolderTopView.setSecondaryProgress(0);
		
		this.mRangeSeekBarView.addOnRangeSeekBarListener(this);
		this.mRangeSeekBarView.addOnRangeSeekBarListener(progressVideoView);
		
		int marge = this.mRangeSeekBarView.getThumbs().get(0).getWidthBitmap();
		int widthSeek = this.mHolderTopView.getThumb().getMinimumWidth() / 2;
		
		LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) this.mHolderTopView.getLayoutParams();
		lp.setMargins(marge - widthSeek, 0, marge - widthSeek, 0);
		this.mHolderTopView.setLayoutParams(lp);
		
		lp = (LinearLayout.LayoutParams) mTimeLineView.getLayoutParams();
		lp.setMargins(marge, 0, marge, 0);
		this.mTimeLineView.setLayoutParams(lp);
		
		lp = (LinearLayout.LayoutParams) progressVideoView.getLayoutParams();
		lp.setMargins(marge, 0, marge, 0);
		progressVideoView.setLayoutParams(lp);
		
		this.mHolderTopView.setOnSeekBarChangeListener(this);
		
		this.mVideoView.setOnPreparedListener(this);
		this.mVideoView.setOnCompletionListener(this);
		this.mVideoView.setOnErrorListener(this);
		
		this.mGestureDetector = new GestureDetector(getContext(), this.mGestureListener);
		this.mVideoView.setOnTouchListener(mTouchListener);
		
		setDefaultDestinationPath();
		this.setShouldProgressBarBeIndeterminate(false);
		this.setShowProgressBarWhileLoadingBitmaps(true,
				500, R.color.colorAccent);
		this.setProgressBarColor();
	}
	
	@SuppressWarnings("unused")
	public void setVideoURI(final Uri videoURI) {
		this.mSrc = videoURI;
		this.getSizeFile(false);
		this.mVideoView.setVideoURI(mSrc);
		this.mVideoView.requestFocus();
		this.mTimeLineView.setThumbnailListener(this.thumbnailListener);
		this.mTimeLineView.setVideo(mSrc);
	}
	
	@SuppressWarnings("unused")
	/**
	 * Please note that this should include the path + filename + extension. IE:
	 * (See README for example)
	 */
	public void setDestinationPath(final String finalPath) {
		if(finalPath != null) {
			if(!finalPath.isEmpty()) {
				this.mFinalPath = finalPath;
				this.userSetCustomOutput = true;
			}
		}
	}
	
	private void setDefaultDestinationPath() {
//		File folder = Environment.getExternalStorageDirectory(); //Adjust default?
		File folder = Environment.getExternalStorageDirectory();
		mFinalPath = folder.getPath() + File.separator;
	}
	
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		int duration = (int) ((mDuration * progress) / 1000L);
		
		if (fromUser) {
			if (duration < mStartPosition) {
				setProgressBarPosition(mStartPosition);
				duration = mStartPosition;
			} else if (duration > mEndPosition) {
				setProgressBarPosition(mEndPosition);
				duration = mEndPosition;
			}
			setTimeVideo(duration);
		}
	}
	
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		mMessageHandler.removeMessages(SHOW_PROGRESS);
		mVideoView.pause();
		mPlayView.setVisibility(View.VISIBLE);
		updateProgress(false);
	}
	
	@Override
	public void onStopTrackingTouch(@NonNull SeekBar seekBar) {
		mMessageHandler.removeMessages(SHOW_PROGRESS);
		mVideoView.pause();
		mPlayView.setVisibility(View.VISIBLE);
		
		int duration = (int) ((mDuration * seekBar.getProgress()) / 1000L);
		mVideoView.seekTo(duration);
		setTimeVideo(duration);
		updateProgress(false);
	}
	
	@Override
	public void onPrepared(@NonNull MediaPlayer mp) {
 /*        Adjust the size of the video
         so it fits on the screen*/
		int videoWidth = mp.getVideoWidth();
		int videoHeight = mp.getVideoHeight();
		if(videoHeight <= 0){
			videoHeight = 1;
		}
		float videoProportion = (float) videoWidth / (float) videoHeight;
		int screenWidth = mLinearVideo.getWidth();
		int screenHeight = mLinearVideo.getHeight();
		if(screenHeight <= 0){
			screenHeight = 1;
		}
		float screenProportion = (float) screenWidth / (float) screenHeight;
		ViewGroup.LayoutParams lp = mVideoView.getLayoutParams();
		
		if (videoProportion > screenProportion) {
			lp.width = screenWidth;
			if(videoProportion <= 0){
				videoProportion = 1;
			}
			lp.height = (int) ((float) screenWidth / videoProportion);
		} else {
			lp.width = (int) (videoProportion * (float) screenHeight);
			lp.height = screenHeight;
		}
		mVideoView.setLayoutParams(lp);
		
		mPlayView.setVisibility(View.VISIBLE);
		
		mDuration = mVideoView.getDuration();
		setSeekBarPosition();
		getSizeFile(false);
		setTimeFrames();
		setTimeVideo(0);
		long croppedFileSize = getCroppedFileSize();
		
		letUserProceed = croppedFileSize < maxFileSize;
		long sizeInKB = getFileSizeInKB();
		if(initialLength <= 0 || (sizeInKB < this.minimumViableVideoSizeInKb)){
			this.mOnTrimVideoListener.invalidVideo();
		}
	}
	
	/**
	 * Get the max file size in Megabytes
	 *
	 * @return
	 */
	public int getMaxFileSize() {
		return maxFileSize;
	}
	
	/**
	 * Set the max file size in MegaBytes (MB)
	 *
	 * @param maxFileSizeInMB Max File size in Megabytes
	 */
	public void setMaxFileSize(int maxFileSizeInMB) {
		this.maxFileSize = maxFileSizeInMB;
	}
	
	/**
	 * The minimum viable video size in KB
	 * @return long
	 */
	public long getMinimumViableVideoSizeInKb() {
		return minimumViableVideoSizeInKb;
	}
	
	/**
	 * This is the minimum viable size in kb to use for trimming. Often times a video is too small
	 * to be used and can cause problems when trying to cut. This is often the result of bad video formats
	 * or things being converted to videos incorrectly. This will default to 250 KB, if you want to
	 * ignore this param, just set this to zero and it will be ignored. Otherwise, if the video size
	 * is initially less than the amount set here, it will ping the error callback:
	 * {@link OnTrimVideoListener#invalidVideo()}
	 * @param minimumViableVideoSizeInKb Minimum size in kilobytes that the INITIAL video should
	 *                                   be in order to be read / parsed
	 */
	public void setMinimumViableVideoSizeInKb(long minimumViableVideoSizeInKb) {
		this.minimumViableVideoSizeInKb = minimumViableVideoSizeInKb;
	}
	
	//region Custom Button Settings
	
	/**
	 * Set the button Details since they currently cannot be touched
	 * @param isSaveButton boolean, if true, these params will affect the 'save button',
	 *                     if false, these params will affect the 'cancel button'.
	 * @param text Text to set. If null, ignored and default it used
	 * @param backgroundColor Background color to set. If null, ignored and default it used
	 * @param textColor Text color to set. If null, ignored and default it used
	 * @param buttonBackgroundDrawable {@link Drawable} Background drawable for the button.
	 *                                                  If null, ignored and default it used
	 * @param useNullTransformationMethod If true, will call
	 *                                    {@link Button#setTransformationMethod(TransformationMethod)}
	 *                                    with a null passed in. The purpose of this is to change the
	 *                                    text from all uppercase to match the actual text passed in
	 *                                    (case included). If null, ignored and default it used of
	 *                                    all capital letters for the text.
	 */
	public void setButtonDetails(boolean isSaveButton,
	                             @Nullable String text,
	                             @Nullable Integer backgroundColor,
	                             @Nullable Integer textColor,
	                             @Nullable Drawable buttonBackgroundDrawable,
	                             @Nullable Boolean useNullTransformationMethod){
		Button btn = (isSaveButton) ? this.viewButtonSave : this.viewButtonCancel;
		if(backgroundColor != null){
			try {
				int background;
				try {
					background = ContextCompat.getColor(this.context, backgroundColor);
				} catch (Resources.NotFoundException nfe){
					background = backgroundColor;
				}
				btn.setBackgroundColor(background);
			} catch (Exception e){
				e.printStackTrace();
			}
		}
		if(textColor != null){
			try {
				int textCol;
				try {
					textCol = ContextCompat.getColor(this.context, textColor);
				} catch (Resources.NotFoundException nfe){
					textCol = textColor;
				}
				btn.setTextColor(textCol);
			} catch (Exception e){
				e.printStackTrace();
			}
		}
		if(buttonBackgroundDrawable != null){
			try {
				btn.setBackground(buttonBackgroundDrawable);
			} catch (Exception e){
				e.printStackTrace();
			}
		}
		if(text != null){
			try {
				btn.setText(text);
			} catch (Exception e){
				e.printStackTrace();
			}
		}
		if(useNullTransformationMethod != null) {
			if(useNullTransformationMethod) {
				btn.setTransformationMethod(null);
			}
		}
	}
	//endregion
	
	//region Custom Drawable Setting Methods
	
	//region Top Seek Bar Drawable
	/**
	 * Set the top seek bar thumb drawable.
	 * This will override the existing one and set whatever is passed
	 * @param drawable
	 */
	public void setTopSeekBarThumbDrawable(@NonNull Drawable drawable){
		if(drawable == null){
			return;
		}
		try {
			this.mHolderTopView.setThumb(drawable);
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Set the top seek bar thumb drawable color.
	 * This will use the existing drawable.
	 * {@link R.drawable#text_select_handle_middle}
	 * @param colorResId
	 */
	public void setTopSeekBarThumbDrawableColor(int colorResId){
		int color = -1;
		try {
			color = ContextCompat.getColor(this.context, colorResId);
		} catch (Resources.NotFoundException e){
			color = colorResId;
		}
		Drawable d = null;
		try {
			d = ContextCompat.getDrawable(this.context, R.drawable.text_select_handle_middle).mutate();
			d.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
			this.mHolderTopView.setThumb(d);
			return;
		} catch (Exception e){
			e.printStackTrace();
		}
		try {
			d = this.getResources().getDrawable(R.drawable.text_select_handle_middle).mutate();
			d.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
			this.mHolderTopView.setThumb(d);
			return;
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	//endregion
	
	//region Bottom Left (Start) and Right (End) Trimmer Drawables
	
	/**
	 * Set the bottom left and right seek bar thumb drawables.
	 * This will override the existing one and set whatever is passed
	 * @param dLeft
	 * @param dRight
	 */
	public void setBottomLeftAndRightSeekBarThumbDrawable(@NonNull Drawable dLeft, @NonNull Drawable dRight){
		try {
			this.mRangeSeekBarView.initThumbsCustom(dLeft, dRight);
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
//	/**
//	 * Set the top seek bar thumb drawable color.
//	 * This will use the existing drawable.
//	 * {@link R.drawable#text_select_handle_middle}
//	 * @param colorResId
//	 */
//	public void setBottomLeftAndRightSeekBarThumbDrawableColor(int colorResId){
//		int color = -1;
//		try {
//			color = ContextCompat.getColor(this.context, colorResId);
//		} catch (Resources.NotFoundException e){
//			e.printStackTrace();
//			color = colorResId;
//		}
//		Drawable dLeft = null, dRight = null;
//		try {
//			dLeft = this.getResources().getDrawable(R.drawable.text_select_handle_left).mutate();
//			dLeft.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
//		} catch (Exception e){
//			e.printStackTrace();
//		}
//		try {
//			dRight = ContextCompat.getDrawable(this.context, R.drawable.select_handle_right).mutate();
//			dRight.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
//		} catch (Exception e){
//			e.printStackTrace();
//		}
//		if(dLeft != null && dRight != null){
//			this.mRangeSeekBarView.initThumbsCustom(dLeft, dRight);
//		} else {
//		}
//	}
	//endregion
	
	//region Bitmap Thumbnail Progress Bar Methods
	
	/**
	 * Get whether the progress bar will show during the loading of bitmaps
	 * @return
	 */
	public boolean getShowProgressBarWhileLoadingBitmaps() {
		return showProgressBarWhileLoadingBitmaps;
	}
	
	/**
	 * Should load an animated progress bar while the bitmap (thumbnails) are loading
	 * @param showProgressBarWhileLoadingBitmaps if true, will show, if false, will not
	 */
	public void setShowProgressBarWhileLoadingBitmaps(boolean showProgressBarWhileLoadingBitmaps) {
		this.setShowProgressBarWhileLoadingBitmaps(showProgressBarWhileLoadingBitmaps, 750);
	}
	
	/**
	 * Should load an animated progress bar while the bitmap (thumbnails) are loading
	 * @param showProgressBarWhileLoadingBitmaps if true, will show, if false, will not
	 * @param timeBetweenAnimationLoads Animated time for each bar load. Defaults to 500
	 *                                  milliseconds per tick / animated chunk.
	 */
	public void setShowProgressBarWhileLoadingBitmaps(boolean showProgressBarWhileLoadingBitmaps, long timeBetweenAnimationLoads) {
		this.setShowProgressBarWhileLoadingBitmaps(showProgressBarWhileLoadingBitmaps, timeBetweenAnimationLoads, R.color.colorAccent);
	}
	
	/**
	 * Should load an animated progress bar while the bitmap (thumbnails) are loading
	 * @param showProgressBarWhileLoadingBitmaps if true, will show, if false, will not
	 * @param timeBetweenAnimationLoads Animated time for each bar load. Defaults to 500
	 *                                  milliseconds per tick / animated chunk.
	 * @param progressBarColorResId int color for the progress bar. Defaults to
	 *                         {@link R.color#colorAccent} / "#FF0800". Pass the color as the int id.
	 */
	public void setShowProgressBarWhileLoadingBitmaps(boolean showProgressBarWhileLoadingBitmaps,
	                                                  long timeBetweenAnimationLoads,
	                                                  int progressBarColorResId) {
		this.showProgressBarWhileLoadingBitmaps = showProgressBarWhileLoadingBitmaps;
		this.animationTimeForThumbnailProgressBar = (timeBetweenAnimationLoads < 0)
				? 500 : timeBetweenAnimationLoads;
		try {
			this.progressBarColor = ContextCompat.getColor(this.context, progressBarColorResId);
		} catch (IllegalArgumentException iae){
			iae.printStackTrace();
			this.progressBarColor = progressBarColorResId;
		} catch (Resources.NotFoundException rnfe){
			rnfe.printStackTrace();
			this.progressBarColor = progressBarColorResId;
		}
		this.setProgressBarColor();
	}
	
	/**
	 * Get whether the progress bar should be indeterminate or not. Defaults to false
	 * @return
	 */
	public boolean getShouldProgressBarBeIndeterminate() {
		return shouldProgressBarBeIndeterminate;
	}
	
	/**
	 * Sets whether or not the progress bar is indeterminate instead of follows a progression
	 * @param shouldProgressBarBeIndeterminate
	 */
	public void setShouldProgressBarBeIndeterminate(boolean shouldProgressBarBeIndeterminate) {
		this.shouldProgressBarBeIndeterminate = shouldProgressBarBeIndeterminate;
	}
	
	/**
	 * Set the Progress Bar Color
	 */
	private void setProgressBarColor(){
		try {
			this.timeLineProgressBar.getIndeterminateDrawable().setColorFilter(
					this.progressBarColor,android.graphics.PorterDuff.Mode.MULTIPLY);
			this.timeLineProgressBar.getProgressDrawable().setColorFilter(
					this.progressBarColor,android.graphics.PorterDuff.Mode.MULTIPLY);
			
			// TODO: 4/29/19 above code workd on indeterminate, check below on determinate
			LayerDrawable progressBarDrawable = (LayerDrawable) this.timeLineProgressBar.getProgressDrawable();
			Drawable backgroundDrawable = progressBarDrawable.getDrawable(0);
			Drawable progressDrawable = progressBarDrawable.getDrawable(1);
			backgroundDrawable.setColorFilter(this.progressBarColor, PorterDuff.Mode.SRC_IN);
			progressDrawable.setColorFilter(this.progressBarColor, PorterDuff.Mode.SRC_IN);
			
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//				ColorStateList colorStateList = ColorStateList.valueOf(this.progressBarColor);
//				this.timeLineProgressBar.setIndeterminateTintList(colorStateList);
//				this.timeLineProgressBar.setIndeterminateTintMode(PorterDuff.Mode.SRC_IN);
//				this.timeLineProgressBar.setProgressTintList(colorStateList);
//				this.timeLineProgressBar.setProgressTintMode(PorterDuff.Mode.SRC_IN);
//				this.timeLineProgressBar.getIndeterminateDrawable().setColorFilter(
//						this.progressBarColor, PorterDuff.Mode.SRC_ATOP);
//				LayerDrawable progressBarDrawable = (LayerDrawable) this.timeLineProgressBar.getProgressDrawable();
//				Drawable backgroundDrawable = progressBarDrawable.getDrawable(0);
//				Drawable progressDrawable = progressBarDrawable.getDrawable(1);
//				backgroundDrawable.setColorFilter(this.progressBarColor, PorterDuff.Mode.SRC_IN);
//				progressDrawable.setColorFilter(this.progressBarColor, PorterDuff.Mode.SRC_IN);
			} else {
//				this.timeLineProgressBar.getIndeterminateDrawable().setColorFilter(
//						this.progressBarColor, PorterDuff.Mode.SRC_IN);
			}
			
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	//endregion
	
	private void setSeekBarPosition() {
		
		if ((mDuration >= mMaxDuration) && (mDuration > 0)) {
			mStartPosition = mDuration / 2 - mMaxDuration / 2;
			mEndPosition = mDuration / 2 + mMaxDuration / 2;
			
			mRangeSeekBarView.setThumbValue(0, (mStartPosition * 100) / mDuration);
			mRangeSeekBarView.setThumbValue(1, (mEndPosition * 100) / mDuration);
			
		} else {
			mStartPosition = 0;
			mEndPosition = mDuration;
		}
		
		setProgressBarPosition(mStartPosition);
		mVideoView.seekTo(mStartPosition);
		
		mTimeVideo = mDuration;
		mRangeSeekBarView.initMaxWidth();
		
		initialLength = ((mEndPosition - mStartPosition) / 1000);
	}
	
	private void startTrimVideo(@NonNull final File file, @NonNull final String dst,
	                            final int startVideo, final int endVideo,
	                            final boolean userDefinedCustomDest,
	                            @NonNull final OnTrimVideoListener callback) {
		
		BackgroundExecutor.execute(
				new BackgroundExecutor.Task("", 0L, "") {
					@Override
					public void execute() {
						try {
							TrimVideoUtils.startTrim(file, dst, startVideo, endVideo, userDefinedCustomDest, callback);
						} catch (final Throwable e) {
							Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
						}
					}
				}
		);
	}
	
	private void setTimeFrames() {
		String seconds = getContext().getString(R.string.short_seconds);
		mTextTimeFrame.setText(String.format(
				"%s %s - %s %s", stringForTime(mStartPosition), seconds,
				stringForTime(mEndPosition), seconds));
	}
	
	
	private void setTimeVideo(int position) {
		String seconds = getContext().getString(R.string.short_seconds);
		mTextTime.setText(String.format("%s %s", stringForTime(position), seconds));
	}
	
	@Override
	public void onCreate(RangeSeekBarView rangeSeekBarView, int index, float value) {
	
	}
	
	@Override
	public void onSeek(RangeSeekBarView rangeSeekBarView, int index, float value) {
 /*        0 is Left selector
         1 is right selector*/
		switch (index) {
			case 0: {
				mStartPosition = (int) ((mDuration * value) / 100L);
				mVideoView.seekTo(mStartPosition);
				break;
			}
			case 1: {
				mEndPosition = (int) ((mDuration * value) / 100L);
				break;
			}
		}
		setProgressBarPosition(mStartPosition);
		
		setTimeFrames();
		getSizeFile(true);
		mTimeVideo = mEndPosition - mStartPosition;
		letUserProceed = getCroppedFileSize() < maxFileSize;
	}
	
	@Override
	public void onSeekStart(RangeSeekBarView rangeSeekBarView, int index, float value) {
	
	}
	
	@Override
	public void onSeekStop(RangeSeekBarView rangeSeekBarView, int index, float value) {
		mMessageHandler.removeMessages(SHOW_PROGRESS);
		mVideoView.pause();
		mPlayView.setVisibility(View.VISIBLE);
	}
	
	private String stringForTime(int timeMs) {
		int totalSeconds = timeMs / 1000;
		
		int seconds = totalSeconds % 60;
		int minutes = (totalSeconds / 60) % 60;
		int hours = totalSeconds / 3600;
		
		Formatter mFormatter = new Formatter();
		if (hours > 0) {
			return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
		} else {
			return mFormatter.format("%02d:%02d", minutes, seconds).toString();
		}
	}
	
	private void getSizeFile(boolean isChanged) {
		if (isChanged) {
			long initSize = getFileSize();
			long newSize;
			if(initialLength <= 0){
				return;
			}
			newSize = ((initSize / initialLength) * (mEndPosition - mStartPosition));
			mTextSize.setText(String.format("%s %s", newSize / 1024, getContext().getString(R.string.megabyte)));
		} else {
			if (mOriginSizeFile == 0) {
				File file = new File(mSrc.getPath());
				mOriginSizeFile = file.length();
				mFileSizeInKB = mOriginSizeFile / 1024;
				if (mFileSizeInKB > 1000) {
					long fileSizeInMB = mFileSizeInKB / 1024;
					mTextSize.setText(String.format("%s %s", fileSizeInMB, getContext().getString(R.string.megabyte)));
				} else {
					mTextSize.setText(String.format("%s %s", mFileSizeInKB, getContext().getString(R.string.kilobyte)));
				}
			}
		}
	}
	
	private long getFileSize() {
		long fileSizeInKB = getFileSizeInKB();
		return fileSizeInKB / 1024;
	}
	
	private long getFileSizeInKB() {
		File file = new File(mSrc.getPath());
		mOriginSizeFile = file.length();
		long fileSizeInKB = mOriginSizeFile / 1024;
		
		return fileSizeInKB;
	}
	
	private long getCroppedFileSize() {
		long initSize = getFileSize();
		long newSize;
		if(initialLength <= 0){
			return 0;
		}
		newSize = ((initSize / initialLength) * (mEndPosition - mStartPosition));
		return newSize / 1024;
	}
	
	@SuppressWarnings("unused")
	public void setOnTrimVideoListener(OnTrimVideoListener onTrimVideoListener) {
		mOnTrimVideoListener = onTrimVideoListener;
	}
	
	public void setMaxDuration(int maxDuration) {
		if (maxDuration == 0) {
			mMaxDuration = (mEndPosition - mStartPosition) * 1000;
		} else if (maxDuration < 0) {
			mMaxDuration = -maxDuration * 1000;
		} else {
			mMaxDuration = maxDuration * 1000;
		}
	}
	
	@Override
	public void onCompletion(MediaPlayer mediaPlayer) {
		mVideoView.seekTo(0);
	}
	
	@Override
	public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
		return false;
	}
	
	private static class MessageHandler extends Handler {
		
		@NonNull
		private final WeakReference<DeepVideoTrimmer> mView;
		
		MessageHandler(DeepVideoTrimmer view) {
			mView = new WeakReference<>(view);
		}
		
		@Override
		public void handleMessage(Message msg) {
			DeepVideoTrimmer view = mView.get();
			if (view == null || view.mVideoView == null) {
				return;
			}
			
			view.updateProgress(true);
			if (view.mVideoView.isPlaying()) {
				sendEmptyMessageDelayed(0, 10);
			}
		}
	}
	
	private void updateProgress(boolean all) {
		if (mDuration <= 0) {
			return;
		}
		
		int position = mVideoView.getCurrentPosition();
		if (all) {
			for (OnProgressVideoListener item : mListeners) {
				item.updateProgress(position, mDuration, ((position * 100) / mDuration));
			}
		} else {
			mListeners.get(1).updateProgress(position, mDuration, ((position * 100) / mDuration));
		}
	}
	
	@Override
	public void updateProgress(int time, int max, float scale) {
		if (mVideoView == null) {
			return;
		}
		
		if (time >= mEndPosition) {
			mMessageHandler.removeMessages(SHOW_PROGRESS);
			mVideoView.pause();
			mPlayView.setVisibility(View.VISIBLE);
			mResetSeekBar = true;
			return;
		}
		
		if (mHolderTopView != null) {
			/*use long to avoid overflow*/
			setProgressBarPosition(time);
		}
		setTimeVideo(time);
	}
	
	
	private void setProgressBarPosition(int position) {
		if (mDuration > 0) {
			long pos = 1000L * position / mDuration;
			mHolderTopView.setProgress((int) pos);
		}
	}
	
	public void destroy() {
		BackgroundExecutor.cancelAll("", true);
		UiThreadExecutor.cancelAll("");
	}
}
