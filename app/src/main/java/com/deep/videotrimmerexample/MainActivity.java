package com.deep.videotrimmerexample;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.deep.videotrimmer.StandaloneVideoTrimmer;
import com.deep.videotrimmer.interfaces.OnTrimVideoListener;
import com.deep.videotrimmer.utils.FileUtils;
import com.deep.videotrimmerexample.databinding.ActivityMainBinding;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.spec.ECField;

import static com.deep.videotrimmerexample.Constants.EXTRA_VIDEO_PATH;

public class MainActivity extends BaseActivity implements View.OnClickListener {
    ActivityMainBinding mBinder;
    public static final int PERMISSION_STORAGE = 100;
    private final int REQUEST_VIDEO_TRIMMER_RESULT = 342;

    private final int REQUEST_VIDEO_TRIMMER = 0x12;
    private File thumbFile;
    private String selectedVideoName = null,selectedVideoFile = null;
    private RequestOptions simpleOptions;
    private boolean isChecked;
    private EditText etStart, etEnd;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinder = DataBindingUtil.setContentView(this, R.layout.activity_main);
        setUpToolbar("Video Trimmer Example");
        mBinder.btnSelectVideo.setOnClickListener(this);
        simpleOptions = new RequestOptions()
                .centerCrop()
                .placeholder(R.color.blackOverlay)
                .error(R.color.blackOverlay)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE);
        final LinearLayout linearLayout = (LinearLayout) this.findViewById(R.id.activity_main_linlay);
	    CheckBox checkbox = (CheckBox) this.findViewById(R.id.checkbox);
	    checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
	    	this.isChecked = isChecked;
		    int visibility = (isChecked) ? View.VISIBLE : View.GONE;
		    linearLayout.setVisibility(visibility);
	    });
	    this.isChecked = false;
	    etStart = (EditText) this.findViewById(R.id.et_start_time);
	    etEnd = (EditText) this.findViewById(R.id.et_end_time);
	    
	    this.etStart.setText("0");
	    this.etEnd.setText("10");
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnSelectVideo:
                checkForPermission();
                break;
        }
    }

    private void checkForPermission() {
        requestAppPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSION_STORAGE, new BaseActivity.setPermissionListener() {
                    @Override
                    public void onPermissionGranted(int requestCode) {
                        selectVideoDialog();
                    }

                    @Override
                    public void onPermissionDenied(int requestCode) {
                        showSnackbar(mBinder.getRoot(), getString(R.string.critical_permission_denied),
                                Snackbar.LENGTH_INDEFINITE, getString(R.string.allow), new OnSnackbarActionListener() {
                                    @Override
                                    public void onAction() {
                                        checkForPermission();
                                    }
                                });
                    }

                    @Override
                    public void onPermissionNeverAsk(int requestCode) {
                        showPermissionSettingDialog(getString(R.string.permission_gallery_camera));
                    }
                });
    }

    private void selectVideoDialog() {
        new VideoPicker(this) {
            @Override
            protected void onCameraClicked() {
                openVideoCapture();
            }

            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            protected void onGalleryClicked() {
                Intent intent = new Intent();
                intent.setTypeAndNormalize("video/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(Intent.createChooser(intent, getString(R.string.select_video)), REQUEST_VIDEO_TRIMMER);
            }
        }.show();
    }

    private void openVideoCapture() {
        Intent videoCapture = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        startActivityForResult(videoCapture, REQUEST_VIDEO_TRIMMER);
    }

    private void startTrimActivity(@NonNull Uri uri) {
        Intent intent = new Intent(this, VideoTrimmerActivity.class);
        intent.putExtra(EXTRA_VIDEO_PATH, FileUtils.getPath(this, uri));
        startActivityForResult(intent, REQUEST_VIDEO_TRIMMER_RESULT);
    }

    private File getFileFromBitmap(Bitmap bmp) {
        if(bmp == null){
            return null;
        }
        /*//create a file to write bitmap data*/
        thumbFile = new File(this.getCacheDir(), "thumb_" + selectedVideoName + ".png");
        try {
            thumbFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*//Convert bitmap to byte array*/
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
        byte[] bitmapdata = bos.toByteArray();
        /*//write the bytes in file*/
        try {
            FileOutputStream fos = new FileOutputStream(thumbFile);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return thumbFile;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_VIDEO_TRIMMER:
                    Uri selectedUri = data.getData();
                    if (selectedUri != null) {
                    	if(this.isChecked){
                    		String fileName = FileUtils.getPath(this, selectedUri);
                    		selectedUri = Uri.parse(fileName);
                    		if(fileName == null){
			                    startTrimActivity(selectedUri);
			                    return;
		                    }
                    		String startTimeStringFromET = this.etStart.getText().toString();
                    		String endTimeStringFromET = this.etEnd.getText().toString();
                    		Integer startTimeInMillisec = null;
		                    Integer endTimeInMillisec = null;
                    		try {
			                    startTimeInMillisec = 1000 * Integer.parseInt(startTimeStringFromET);
		                    } catch (Exception e){
                    			startTimeInMillisec = null;
		                    }
		                    try {
			                    endTimeInMillisec = 1000 * Integer.parseInt(endTimeStringFromET);
		                    } catch (Exception e){
			                    endTimeInMillisec = null;
		                    }
                    		fileName = fileName.replace(".mp4", "_standalonetest.mp4");
		                    try {
			                    OnTrimVideoListener listener = new OnTrimVideoListener() {
				                    @Override
				                    public void invalidVideo() {
				                    	MainActivity.this.runOnUiThread(() -> Toast.makeText(
				                    			MainActivity.this, "An error has occurred",
							                    Toast.LENGTH_SHORT).show());
				                    }
				
				                    @Override
				                    public void getResult(Uri uri) {
					                    if(uri != null){
						                    selectedVideoFile = uri.getPath();
						                    selectedVideoName = uri.getLastPathSegment();
						                    loadResultUri(uri);
					                    }
				                    }
				
				                    @Override
				                    public void cancelAction() {
					                    MainActivity.this.runOnUiThread(() -> Toast.makeText(
							                    MainActivity.this, "User cancelled action",
							                    Toast.LENGTH_SHORT).show());
				                    }
			                    };
			                    StandaloneVideoTrimmer.trimVideo(this, listener,
					                    selectedUri, fileName, startTimeInMillisec, endTimeInMillisec);
		                    } catch (Exception e){
			                    e.printStackTrace();
		                    }
		                    
	                    } else {
		                    startTrimActivity(selectedUri);
	                    }
                    } else {
                        showToastShort(getString(R.string.toast_cannot_retrieve_selected_video));
                    }
                    break;
                case REQUEST_VIDEO_TRIMMER_RESULT:
                    final Uri selectedVideoUri = data.getData();

                    if (selectedVideoUri != null) {
                        selectedVideoFile = data.getData().getPath();
                        selectedVideoName = data.getData().getLastPathSegment();
	
	                    loadResultUri(selectedVideoUri);
                    } else {
                        showToastShort(getString(R.string.toast_cannot_retrieve_selected_video));
                    }
                    break;
            }
        }
    }
	
	/**
	 * Load Result Uri into position
	 * @param selectedVideoUri
	 */
	private void loadResultUri(Uri selectedVideoUri){
	    if (selectedVideoUri != null) {
	    	String path = selectedVideoUri.getPath();
		    MainActivity.this.runOnUiThread(() -> Toast.makeText(
				    MainActivity.this, "Success. Path: " + path,
				    Toast.LENGTH_LONG).show());
		    Bitmap thumb = ThumbnailUtils.createVideoThumbnail(selectedVideoUri.getPath(),
				    MediaStore.Images.Thumbnails.FULL_SCREEN_KIND);
		    Glide.with(this)
				    .load(getFileFromBitmap(thumb))
				    .apply(simpleOptions)
				    .into(mBinder.selectedVideoThumb);
	    } else {
		    showToastShort(getString(R.string.toast_cannot_retrieve_selected_video));
	    }
    }
}
