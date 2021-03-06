package com.martin.ads.vrlib;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.martin.ads.vrlib.constant.PanoMode;
import com.martin.ads.vrlib.constant.PanoStatus;
import com.martin.ads.vrlib.filters.advanced.FilterType;
import com.martin.ads.vrlib.utils.UIUtils;

/**
 * Created by Ads on 2016/11/10.
 * UI is modified from UtoVR demo
 */
//FIXME:looks so lame.
public class PanoPlayerActivity extends Activity {

    public static final String FILE_PATH = "filePath";
    public static final String IMAGE_MODE = "imageMode";
    public static final String PLANE_MODE = "planeMode";
    public static final String WINDOW_MODE = "windowMode";

    private PanoUIController mPanoUIController;
    private PanoViewWrapper mPanoViewWrapper;
    private ImageView mImgBufferAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.player_activity_layout);

        init();
    }

    private void init(){
        String filePath = getIntent().getStringExtra(FILE_PATH);
        boolean imageMode = getIntent().getBooleanExtra(IMAGE_MODE, false);
        boolean planeMode = getIntent().getBooleanExtra(PLANE_MODE, false);
        boolean windowMode = getIntent().getBooleanExtra(WINDOW_MODE, false);

        findViewById(R.id.img_full_screen).setVisibility(windowMode ? View.VISIBLE : View.GONE);

        mImgBufferAnim = (ImageView) findViewById(R.id.activity_imgBuffer);
        UIUtils.setBufferVisibility(mImgBufferAnim, !imageMode);
        mPanoUIController = new PanoUIController(
                (RelativeLayout)findViewById(R.id.player_toolbar_control),
                (RelativeLayout)findViewById(R.id.player_toolbar_progress),
                this, imageMode);

        TextView title = (TextView) findViewById(R.id.video_title);
        title.setText(Uri.parse(filePath).getLastPathSegment());

        GLSurfaceView glSurfaceView = (GLSurfaceView) findViewById(R.id.surface_view);
        boolean removeHotspot=getIntent().getBooleanExtra("removeHotspot",false);
        mPanoViewWrapper = PanoViewWrapper.with(this)
                .setFilePath(filePath)
                .setGlSurfaceView(glSurfaceView)
                .setImageMode(imageMode)
                .setPlaneMode(planeMode)
                .init();
        if(removeHotspot) mPanoViewWrapper.removeDefaultHotSpot();
        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mPanoUIController.startHideControllerTimer();
                return mPanoViewWrapper.handleTouchEvent(event);
            }
        });
        mPanoUIController.setAutoHideController(true);
        mPanoUIController.setUiCallback(new PanoUIController.UICallback() {
            @Override
            public void requestScreenshot() {
                mPanoViewWrapper.getTouchHelper().shotScreen();
            }

            @Override
            public void requestFinish() {
                finish();
            }

            @Override
            public void changeDisPlayMode() {
                if (mPanoViewWrapper.getStatusHelper().getPanoDisPlayMode()==PanoMode.DUAL_SCREEN)
                    mPanoViewWrapper.getStatusHelper().setPanoDisPlayMode(PanoMode.SINGLE_SCREEN);
                else mPanoViewWrapper.getStatusHelper().setPanoDisPlayMode(PanoMode.DUAL_SCREEN);
            }

            @Override
            public void changeInteractiveMode() {
                if (mPanoViewWrapper.getStatusHelper().getPanoInteractiveMode()==PanoMode.MOTION)
                    mPanoViewWrapper.getStatusHelper().setPanoInteractiveMode(PanoMode.TOUCH);
                else mPanoViewWrapper.getStatusHelper().setPanoInteractiveMode(PanoMode.MOTION);
            }

            @Override
            public void changePlayingStatus() {
                if (mPanoViewWrapper.getStatusHelper().getPanoStatus()== PanoStatus.PLAYING){
                    mPanoViewWrapper.getMediaPlayer().pauseByUser();
                }else if (mPanoViewWrapper.getStatusHelper().getPanoStatus()== PanoStatus.PAUSED_BY_USER){
                    mPanoViewWrapper.getMediaPlayer().start();
                }
            }

            @Override
            public void playerSeekTo(int pos) {
                mPanoViewWrapper.getMediaPlayer().seekTo(pos);
            }

            @Override
            public int getPlayerDuration() {
                return mPanoViewWrapper.getMediaPlayer().getDuration();
            }

            @Override
            public int getPlayerCurrentPosition() {
                return mPanoViewWrapper.getMediaPlayer().getCurrentPosition();
            }

            @Override
            public void addFilter(FilterType filterType) {
                mPanoViewWrapper.getRenderer().switchFilter();
            }
        });
        mPanoViewWrapper.getTouchHelper().setPanoUIController(mPanoUIController);

        if(!imageMode){
            mPanoViewWrapper.getMediaPlayer().setPlayerCallback(new PanoMediaPlayerWrapper.PlayerCallback() {
                @Override
                public void updateProgress() {
                    mPanoUIController.updateProgress();
                }

                @Override
                public void updateInfo() {
                    UIUtils.setBufferVisibility(mImgBufferAnim,false);
                    mPanoUIController.startHideControllerTimer();
                    mPanoUIController.setInfo();
                }

                @Override
                public void requestFinish() {
                    finish();
                }
            });
        }else mPanoUIController.startHideControllerTimer();
    }

    @Override
    protected void onPause(){
        super.onPause();
        mPanoViewWrapper.onPause();
    }

    @Override
    protected void onResume(){
        super.onResume();
        mPanoViewWrapper.onResume();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        mPanoViewWrapper.releaseResources();
    }
}
