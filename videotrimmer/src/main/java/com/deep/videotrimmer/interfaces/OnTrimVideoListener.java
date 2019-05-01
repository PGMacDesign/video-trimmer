package com.deep.videotrimmer.interfaces;

import android.net.Uri;

/**
 * Created by Deep Patel
 * (Sr. Android Developer)
 * on 6/4/2018
 */
public interface OnTrimVideoListener {
    
    /**
     * This will trigger if the video passed is invalid or cannot be properly read
     */
    void invalidVideo();
    
    /**
     * Get the result Uri of the written video
     * @param uri
     */
    void getResult(final Uri uri);
    
    /**
     * User clicked cancel (Cancel the current state)
     */
    void cancelAction();
}
