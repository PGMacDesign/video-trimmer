package com.deep.videotrimmer.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;

public class ImageUtils {
	
	/**
	 * Convert a Drawable to a Bitmap
	 * @param drawableResId Drawable resource ID (IE: R.drawable.something)
	 * @return Converted Bitmap
	 */
	public static Bitmap convertDrawableToBitmap(Context context, int drawableResId){
		try {
			return convertDrawableToBitmap(ContextCompat.getDrawable(context, drawableResId));
		} catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Convert a Drawable to a Bitmap
	 * @param drawable Drawable to convert
	 * @return Converted Bitmap
	 */
	public static Bitmap convertDrawableToBitmap(Drawable drawable){
		if(drawable == null){
			return null;
		}
		Bitmap bitmap = null;
		if (drawable instanceof BitmapDrawable) {
			BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
			if(bitmapDrawable.getBitmap() != null) {
				return bitmapDrawable.getBitmap();
			}
		}
		
		if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
			bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
		} else {
			bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		}
		
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return bitmap;
	}
	
}
