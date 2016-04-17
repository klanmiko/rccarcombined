package cf.makesc.bluetoothtesting;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 * Created by Kentaro on 3/22/2015.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback , Camera.PreviewCallback {
    private Camera mCamera;
    private SurfaceHolder mHolder;
    private Socket socket;
    private DatagramChannel out;
    long oldtime,newtime,delta;
    private static final int SERVERPORT = 56469;
    private static final String SERVER_IP = "192.168.1.126";
    private static final int WIDTH = 320;
    private static final int HEIGHT = 240;
    private InetAddress host;

    public CameraPreview(Context context, Camera camera,InetAddress host) {
        super(context);
        if(host!=null)
        {
            this.host = host;
        }
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
                    Log.i("host",host.toString());
                    socket = new Socket();
                    if(host==null||host.isAnyLocalAddress()||host.isLoopbackAddress()) {
                        InetAddress serverAddress = InetAddress.getByName(SERVER_IP);
                        socket.connect(new InetSocketAddress(serverAddress,SERVERPORT));
                    }
                    else{
                        socket.connect(new InetSocketAddress(host,SERVERPORT));
                    }
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
            mCamera.setDisplayOrientation(0);
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
        } catch (Exception e) {
            e.printStackTrace();
        }

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
        newtime=System.currentTimeMillis();
        delta=newtime-oldtime;
        //Log.i("fps",String.valueOf(delta));
        if (socket != null) {
            if (socket.isConnected()){
                try{
                        Camera.Parameters p = mCamera.getParameters();
                        //p.setPreviewSize(WIDTH, HEIGHT);
                        //mCamera.setParameters(p);
                        YuvImage yuvImage = new YuvImage(data, p.getPreviewFormat(), p.getPreviewSize().width, p.getPreviewSize().height, null); //makes YUVImage from the preview image
                        Rect rect = new Rect(0, 0, p.getPreviewSize().width, p.getPreviewSize().height);
                        ByteArrayOutputStream tempStream = new ByteArrayOutputStream();
                        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                        yuvImage.compressToJpeg(rect, 30, tempStream); //sends YUVImage to bytearrayoutputstream
                        byte[] temp = tempStream.toByteArray(); //temp is a byte[] that represents the image
                        byte[] start = {0x48, 0x41, 0x23};
                        dataOutputStream.write(start,0,3);
                        dataOutputStream.writeInt(temp.length);
                    dataOutputStream.write(temp,0,temp.length);
                        dataOutputStream.flush();

                        //writes the image byte[] using dataoutputstream

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        oldtime=newtime;
    }
}
