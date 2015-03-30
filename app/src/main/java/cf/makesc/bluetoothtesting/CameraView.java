package cf.makesc.bluetoothtesting;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.jar.Attributes;

/**
 * Created by Kentaro on 3/29/2015.
 */
public class CameraView extends SurfaceView implements SurfaceHolder.Callback{



    //currently not used, probably has bugs but we are substituting for an image view for now





    UpdateThread updateThread;

    private void init() {
        getHolder().addCallback(this);
        updateThread = new UpdateThread(getHolder(),this);
    }

    public CameraView (Context context) {
        super(context);
        init();
    }

    public CameraView (Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    public CameraView (Context context, AttributeSet attributeSet, int defStyle) {
        super(context, attributeSet, defStyle);
        init();
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        Bitmap image = ServerActivity.image;
        c.drawColor(Color.BLACK);
        if (image != null) {
            c.drawBitmap(image,10,10,null);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        updateThread.setRun(true);
        updateThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        updateThread.setRun(false);
        while (retry) {
            try {
                updateThread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }

    class UpdateThread extends Thread {
        private SurfaceHolder surfaceHolder;
        private CameraView cameraView;
        private boolean run;

        public UpdateThread(SurfaceHolder s, CameraView c) {
            cameraView = c;
            surfaceHolder = s;
        }

        public void setRun (boolean r) {
            run = r;
        }

        @Override
        public void run() {
            Canvas c;
            while (run) {
                c = null;
                try {
                    c = surfaceHolder.lockCanvas();
                    synchronized (surfaceHolder) {
                        cameraView.onDraw(c);
                    }
                } finally {
                    if (c!= null) {
                        surfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }
    }
}
