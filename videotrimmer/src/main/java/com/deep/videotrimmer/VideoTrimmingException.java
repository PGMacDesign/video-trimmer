package com.deep.videotrimmer;

/**
 * Thrown whenever certain localized issues occur in the {@link StandaloneVideoTrimmer} class
 */
public class VideoTrimmingException extends Exception {
	
	public VideoTrimmingException(){
		super();
	}
	
	public VideoTrimmingException(String str){
		super(str);
	}
}
