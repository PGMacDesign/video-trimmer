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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import com.deep.videotrimmer.R;
import com.deep.videotrimmer.utils.ImageUtils;
import com.deep.videotrimmer.utils.L;

import java.util.List;
import java.util.Vector;
/**
 * Created by Deep Patel
 * (Sr. Android Developer)
 * on 6/4/2018
 */
public class Thumb {

    private int mIndex;
    private float mVal;
    private float mPos;
    private Bitmap mBitmap;
    private int mWidthBitmap;
    private int mHeightBitmap;

    private float mLastTouchX;

    private Thumb() {
        mVal = 0;
        mPos = 0;
    }

    public int getIndex() {
        return mIndex;
    }

    private void setIndex(int index) {
        mIndex = index;
    }

    public float getVal() {
        return mVal;
    }

    public void setVal(float val) {
        mVal = val;
    }

    public float getPos() {
        return mPos;
    }

    public void setPos(float pos) {
        mPos = pos;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    private void setBitmap(@NonNull Bitmap bitmap) {
        mBitmap = bitmap;
        mWidthBitmap = bitmap.getWidth();
        mHeightBitmap = bitmap.getHeight();
    }

    @NonNull
    public static List<Thumb> initThumbs(Resources resources) {

        List<Thumb> thumbs = new Vector<>();

        for (int i = 0; i < 2; i++) {
            Thumb th = new Thumb();
            th.setIndex(i);
            if (i == 0) {
                int resImageLeft = R.drawable.text_select_handle_left;
                th.setBitmap(BitmapFactory.decodeResource(resources, resImageLeft));
            } else {
                int resImageRight = R.drawable.select_handle_right;
                th.setBitmap(BitmapFactory.decodeResource(resources, resImageRight));
            }

            thumbs.add(th);
        }

        return thumbs;
    }
    
    /**
     * Set the left and right drawables respectively
     * @param drawableLeft Left Side Drawable
     * @param drawableRight Right Side Drawable
     */
    public static List<Thumb> initThumbsCustom(Resources resources, @NonNull Drawable drawableLeft, @NonNull Drawable drawableRight){
    
        List<Thumb> thumbs = new Vector<>();
    
        try {
            for (int i = 0; i < 2; i++) {
                Thumb th = new Thumb();
                th.setIndex(i);
                if (i == 0) {
                    Bitmap b = ImageUtils.convertDrawableToBitmap(drawableLeft);
                    if(b == null){
                        L.m("Bitmap is null (LEFT), going back to default");
                        return initThumbs(resources);
                    }
                    th.setBitmap(b);
                } else {
                    Bitmap b = ImageUtils.convertDrawableToBitmap(drawableRight);
                    if(b == null){
                        L.m("Bitmap is null (RIGHT), going back to default");
                        return initThumbs(resources);
                    }
                    th.setBitmap(b);
                }
        
                thumbs.add(th);
            }
            L.m("Successfully updated thumbs! @149");
            return thumbs;
        } catch (Exception e){
            e.printStackTrace();
        }
        L.m("did not successfully update thumbs! @154");
        return Thumb.initThumbs(resources);
    }

    public static int getWidthBitmap(@NonNull List<Thumb> thumbs) {
        return thumbs.get(0).getWidthBitmap();
    }

    public static int getHeightBitmap(@NonNull List<Thumb> thumbs) {
        return thumbs.get(0).getHeightBitmap();
    }

    public float getLastTouchX() {
        return mLastTouchX;
    }

    public void setLastTouchX(float lastTouchX) {
        mLastTouchX = lastTouchX;
    }

    public int getWidthBitmap() {
        return mWidthBitmap;
    }

    private int getHeightBitmap() {
        return mHeightBitmap;
    }
}
