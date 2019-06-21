package com.deep.videotrimmerexample;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.deep.videotrimmer.DeepVideoTrimmer;
import com.deep.videotrimmer.interfaces.OnTrimVideoListener;
import com.deep.videotrimmer.view.RangeSeekBarView;
import com.deep.videotrimmerexample.databinding.ActivityVideoTrimmerBinding;

import static com.deep.videotrimmerexample.Constants.EXTRA_VIDEO_PATH;

public class VideoTrimmerActivity extends BaseActivity implements OnTrimVideoListener {
    ActivityVideoTrimmerBinding mBinder;
    private DeepVideoTrimmer mVideoTrimmer;
    TextView textSize, tvCroppingMessage;
    RangeSeekBarView timeLineBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinder = DataBindingUtil.setContentView(this, R.layout.activity_video_trimmer);

        Intent extraIntent = getIntent();
        String path = "";

        if (extraIntent != null) {
            path = extraIntent.getStringExtra(EXTRA_VIDEO_PATH);
        }
        mVideoTrimmer = ((DeepVideoTrimmer) findViewById(R.id.timeLine));
        timeLineBar = (RangeSeekBarView) findViewById(R.id.timeLineBar);
        textSize = (TextView) findViewById(R.id.textSize);
        tvCroppingMessage = (TextView) findViewById(R.id.tvCroppingMessage);
        if (mVideoTrimmer != null && path != null) {
            //Setting the bottom left and right seek bar drawables
//            mVideoTrimmer.setBottomLeftAndRightSeekBarThumbDrawable(
//                    ContextCompat.getDrawable(this, R.drawable.ic_launcher_background),
//                    ContextCompat.getDrawable(this, R.drawable.ic_launcher_background));
            //Setting the button details (text, color, etc)
//            mVideoTrimmer.setButtonDetails(true, "s Yo", R.color.line_color,
//                    android.R.color.holo_blue_bright,
//                    null, true);
            mVideoTrimmer.setMaxDuration(100);
            mVideoTrimmer.setOnTrimVideoListener(new OnTrimVideoListener() {
                @Override
                public void invalidVideo() {
                    Toast.makeText(VideoTrimmerActivity.this, "INVALID VIDEO!", Toast.LENGTH_LONG).show();
                }
    
                @Override
                public void getResult(Uri uri) {
                    //Do stuff here with the result
                }
    
                @Override
                public void cancelAction() {
                    //Do stuff here to cancel
                }
            });
            mVideoTrimmer.setOnTrimVideoListener(this);
            mVideoTrimmer.setVideoURI(Uri.parse(path));
        } else {
            showToastLong(getString(R.string.toast_cannot_retrieve_selected_video));
        }
        this.mVideoTrimmer.hideButtons(false);
//        this.mVideoTrimmer.setOnLongClickListener(v -> {
//	        Log.d("d", "Start position == " + (mVideoTrimmer.getStartPosition()));
//	        Log.d("d", "End position == " + (mVideoTrimmer.getEndPosition()));
//	        return true;
//        });
    }
    
    @Override
    public void invalidVideo() {
        try {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(VideoTrimmerActivity.this,
                            "Invalid Video Passed!", Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e){}
    }
    
    @Override
    public void getResult(final Uri uri) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvCroppingMessage.setVisibility(View.GONE);
            }
        });
        Constants.croppedVideoURI = uri.toString();
        Intent intent = new Intent();
        intent.setData(uri);
        setResult(RESULT_OK, intent);
        finish();

    }

    @Override
    public void cancelAction() {
        mVideoTrimmer.destroy();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvCroppingMessage.setVisibility(View.GONE);
            }
        });
        finish();
    }
}
