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
package com.deep.videotrimmer.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.LongSparseArray;
import android.view.View;
import android.widget.ProgressBar;

import com.deep.videotrimmer.R;
import com.deep.videotrimmer.interfaces.ThumbnailGeneratingListener;
import com.deep.videotrimmer.utils.BackgroundExecutor;
import com.deep.videotrimmer.utils.UiThreadExecutor;

/**
 * Created by Deep Patel
 * (Sr. Android Developer)
 * on 6/4/2018
 */

public class TimeLineView extends View {
	
	private ProgressBar progressBar;
	private Context context;
	
	private Uri mVideoUri;
	private int mHeightView, numberOfThumbnailsToGenerate;
	private ThumbnailGeneratingListener listener;
	private LongSparseArray<Bitmap> mBitmapList = null;
	
	public TimeLineView(@NonNull Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public TimeLineView(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		this.context = context;
		init();
	}
	
	private void init() {
		this.listener = null;
		this.numberOfThumbnailsToGenerate = 0;
		this.mHeightView = getContext().getResources().getDimensionPixelOffset(R.dimen.frames_video_height);
		this.progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final int minW = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();
		int w = resolveSizeAndState(minW, widthMeasureSpec, 1);
		
		final int minH = getPaddingBottom() + getPaddingTop() + mHeightView;
		int h = resolveSizeAndState(minH, heightMeasureSpec, 1);
		
		setMeasuredDimension(w, h);
	}
	
	@Override
	protected void onSizeChanged(final int w, int h, final int oldW, int oldH) {
		super.onSizeChanged(w, h, oldW, oldH);
		
		if (w != oldW) {
			getBitmap(w);
		}
	}
	
	/**
	 * This triggers and the bitmaps are created for preview
	 * @param viewWidth
	 */
	private void getBitmap(final int viewWidth) {
		BackgroundExecutor.execute(
				new BackgroundExecutor.Task("", 0L, "") {
					@Override
					public void execute() {
						try {
							LongSparseArray<Bitmap> thumbnailList = new LongSparseArray<>();
							
							MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
							mediaMetadataRetriever.setDataSource(getContext(), mVideoUri);
							
							/* Retrieve media data*/
							long videoLengthInMs = Integer.parseInt(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) * 1000;
							
							/*Set thumbnail properties (Thumbs are squares)*/
							final int thumbWidth = mHeightView;
							final int thumbHeight = mHeightView;
							
							int numThumbs = (int) Math.ceil(((float) viewWidth) / thumbWidth);
							TimeLineView.this.numberOfThumbnailsToGenerate = numThumbs;
							final long interval = videoLengthInMs / numThumbs;
							int numberOfFailedThumbnails = 0;
							for (int i = 0; i < numThumbs; ++i) {
								sendPingOnListenerOnUIThread((1+i), numThumbs);
								Bitmap bitmap = mediaMetadataRetriever.getFrameAtTime(i * interval, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
								try {
									bitmap = Bitmap.createScaledBitmap(bitmap, thumbWidth, thumbHeight, false);
								} catch (NullPointerException npe){
									numberOfFailedThumbnails++;
								} catch (Exception e) {
									e.printStackTrace();
								}
								thumbnailList.put(i, bitmap);
							}
							sendPingOnListenerOnUIThread(numThumbs, numThumbs);
							if(numberOfFailedThumbnails >= (numThumbs - 1)){
								//this means there were almost no thumbnails generated, likely bad video
								pingBadVideoCallback();
							}
							mediaMetadataRetriever.release();
							returnBitmaps(thumbnailList);
						} catch (final Throwable e) {
							Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
						}
					}
				}
		);
	}
	
	/**
	 * Send a callback on a listener for how many items have been generated
	 * @param whichOneHit Which one was made (IE 0)
	 * @param outOfHowMany Out of how many (IE 10)
	 */
	private void sendPingOnListenerOnUIThread(final int whichOneHit, final int outOfHowMany){
		if(this.listener == null){
			return;
		}
		UiThreadExecutor.runTask("",
				new Runnable() {
					@Override
					public void run() {
						listener.thumbnailGenerated(whichOneHit, outOfHowMany);
					}
				}, 0L);
	}
	
	/**
	 * Send a callback on a listener to indicate a bad video
	 */
	private void pingBadVideoCallback(){
		if(this.listener == null){
			return;
		}
		UiThreadExecutor.runTask("",
				new Runnable() {
					@Override
					public void run() {
						listener.couldNotGenerateThumbnails();
					}
				}, 0L);
	}
	
	private void returnBitmaps(final LongSparseArray<Bitmap> thumbnailList) {
		UiThreadExecutor.runTask("",
				new Runnable() {
					@Override
					public void run() {
						mBitmapList = thumbnailList;
						invalidate();
					}
				}, 0L);
	}
	
	@Override
	protected void onDraw(@NonNull Canvas canvas) {
		super.onDraw(canvas);
		if (mBitmapList != null) {
			this.progressBar.setVisibility(GONE);
			canvas.save();
			int x = 0;
			
			for (int i = 0; i < mBitmapList.size(); i++) {
				Bitmap bitmap = mBitmapList.get(i);
				
				if (bitmap != null) {
					canvas.drawBitmap(bitmap, x, 0, null);
					x = x + bitmap.getWidth();
				}
			}
		} else {
			this.progressBar.setIndeterminate(true);
			this.progressBar.setVisibility(VISIBLE);
		}
	}
	
	public void setThumbnailListener(@NonNull ThumbnailGeneratingListener listener) {
		this.listener = listener;
	}
	
	public void setVideo(@NonNull Uri data) {
		mVideoUri = data;
	}
}
