
 [![JitPack](https://jitpack.io/v/pgmacdesign/video-trimmer.svg)](https://jitpack.io/#pgmacdesign/video-trimmer)


# Screenshots:
**Screenshot 1 :**

<img src="https://github.com/deepandroid/video-trimmer/blob/master/images/device-2018-06-06-170717.png" alt="Video Trimmer Screenshot 1" width="360" height="640" />


**Screenshot 2 :**

<img src="https://github.com/deepandroid/video-trimmer/blob/master/images/device-2018-06-06-170642.png" alt="Video Trimmer Screenshot 2" width="360" height="640" />


**Screenshot 3 :**

<img src="https://github.com/deepandroid/video-trimmer/blob/master/images/device-2018-06-06-170736.png" alt="Video Trimmer Screenshot 3" width="360" height="640" />


# Video Trimmer
Whatsapp like video trimmer to trim videos within a defined file size.

# Add in your project

**Gradle :**

Add This to your top level gradle file:

```
allprojects {
    repositories {
        .
        .
        .
        maven { url 'https://jitpack.io' }
    }
}
```

Add this to your module level / app level file:

```
implementation 'com.github.PGMacDesign:video-trimmer:1.2.2'
```   

>**Note:** If you have jCenter() added, then no need to write maven dependancy. only using implementation line it will be integrated.

**XML :**


     <com.deep.videotrimmer.DeepVideoTrimmer
          android:layout_width="match_parent"
          android:layout_height="match_parent" />

# **Customization Settings :**

Customize the 'Save' and 'Cancel' buttons to your heart's content:
```
setButtonDetails(boolean isSaveButton,
                   @Nullable String text,
                   @Nullable Integer backgroundColor,
                   @Nullable Integer textColor,
                   @Nullable Drawable buttonBackgroundDrawable,
                   @Nullable Boolean useNullTransformationMethod);
```                   
                   
Customize the Top Seek bar Drawable or color of the default one:

```
setTopSeekBarThumbDrawable(Drawable)
```
```
setTopSeekBarThumbDrawableColor(colorResId)
```                  

Customize the Drawables used for the Start and End Video Trim images:
```
setBottomLeftAndRightSeekBarThumbDrawable(Drawable dLeft, Drawable dRight)
```

Customize the Progress loading bar when loading Bitmap Thumbnails, the length of time for each animation load, and the color of the progress bar:
```
setShowProgressBarWhileLoadingBitmaps(showProgressBarWhileLoadingBitmaps)
```
```
setShowProgressBarWhileLoadingBitmaps(showProgressBarWhileLoadingBitmaps, timeBetweenAnimationLoads)
```
```
setShowProgressBarWhileLoadingBitmaps(showProgressBarWhileLoadingBitmaps, timeBetweenAnimationLoads, progressBarColorResId)
```

Customize whether the Progress loading bar when loading Bitmap Thumbnails should be indeterminate or not:
```
setShouldProgressBarBeIndeterminate(boolean)
```

Customize the minimum required Size (In Kilobytes) required for a video to be valid. (Useful for poorly transcoded videos or invalid ones)
```
setMinimumViableVideoSizeInKb(long)   //Defaults to 250KB
```

Set your own path to save trimmed videos: 
```
setDestinationPath(StringPath);
```
A good, workable example would be: `"/storage/emulated/0/Movies/MyApp_Edited_2020-12-31-09_02_31.mp4"`
A bad, non-working example (Will throw an exception) would be: `"file:///storage/emulated/0/Movies/MyApp_Edited_2020-12-31-09_02_31.mp4"`

Set your desired max duration for trimmed videos:
```
setMaxDuration(int seconds);  //Defaults to 100Seconds
```

Set your desired max file size for trimmed videos:
```
setMaxFileSize(int mb);   //Defaults to 25Mb
```

Set your desired video URI to begin the video trimming process:
```
setVideoURI(Uri for video to trim);
```

Please note that the above call is required and is the trigger to start the trimming operation; it should likely be called last, after the other configurations have been set.
