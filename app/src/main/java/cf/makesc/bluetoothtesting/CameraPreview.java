package cf.makesc.bluetoothtesting;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by Kentaro on 3/22/2015.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback , Camera.PreviewCallback {
    private Camera mCamera;
    private SurfaceHolder mHolder;
    private Socket socket;
    private DataOutputStream out;

    private static final int SERVERPORT = 56469;
    private static final String SERVER_IP = "192.168.0.42";
    private static final int WIDTH = 320;
    private static final int HEIGHT = 240;


    public CameraPreview(Context context, Camera camera) {
        super(context);

        mCamera = camera;
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InetAddress serverAddress = InetAddress.getByName(SERVER_IP);
                    socket = new Socket (serverAddress, SERVERPORT);
                    out = new DataOutputStream(socket.getOutputStream());

                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mHolder.getSurface() == null) {
            return;
        }

        try {
            mCamera.stopPreview();
        } catch (Exception e) {}

        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) { //called whenever a frame is called up on the camera preview
        if (socket.isConnected()) {
            try {
                Camera.Parameters p = mCamera.getParameters();
                p.setPreviewSize(WIDTH, HEIGHT);
                mCamera.setParameters(p);
                YuvImage yuvImage = new YuvImage(data, p.getPreviewFormat(), WIDTH, HEIGHT, null); //makes YUVImage from the preview image
                Rect rect = new Rect(0,0,WIDTH, HEIGHT);
                ByteArrayOutputStream tempStream = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(rect, 50, tempStream); //sends YUVImage to bytearrayoutputstream
                byte[] temp = tempStream.toByteArray(); //temp is a byte[] that represents the image
                out.writeInt(temp.length); //writes the length of temp using dataoutputstream
                out.write(temp); //writes the image byte[] using dataoutputstream

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
