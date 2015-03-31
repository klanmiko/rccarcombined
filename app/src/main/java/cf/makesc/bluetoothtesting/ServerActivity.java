package cf.makesc.bluetoothtesting;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;


public class ServerActivity extends Activity {

    private ServerSocket serverSocket;
    public static Bitmap image = null;
    static final int PORT = 56469;
    ImageView imageView;
    public Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        imageView = (ImageView) findViewById(R.id.cam_image);
        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message inputMessage) {
                // Gets the image task from the incoming Message object.
                Bitmap bm = (Bitmap)inputMessage.obj;
                Log.i("drawing","draw");
                imageView.setImageBitmap(bm);
                super.handleMessage(inputMessage);
            }

        };
        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ServerThread serverThread = new ServerThread(this);
        serverThread.start();

    }
    public void setBitmap(Bitmap bm)
    {
        imageView.setImageBitmap(bm);
    }
    class ServerThread extends Thread {
        ServerActivity parent;
        public ServerThread(ServerActivity activity)
        {
            parent=activity;
        }
        public void run()
        {
            while(true)
            {
                try
                {
                    Log.i("currently","waiting");
                    Socket server = serverSocket.accept(); //should probably switch the sides that do accept()
                    DataInputStream inStream = new DataInputStream(server.getInputStream());
                    Log.i("connected","successful");
                    while (server.isConnected()) {
                        if (inStream.available()>0) {
                            byte f = inStream.readByte();
                            while(f!=0x54) {
                                if(server.isConnected())
                                    f=inStream.readByte();
                                else
                                    break;
                            }
                            int size =  inStream.readUnsignedShort(); //read image size
                            Log.i("stuff","reading");
                            byte[] imageBytes = new byte[size];
                            int counter=0;
                            inStream.read(imageBytes,0,size);
                            Bitmap bm = BitmapFactory.decodeByteArray(imageBytes, 0, size);
                            Message frame = handler.obtainMessage(1,bm);
                            frame.sendToTarget();
                            //here i set imageview to a bitmap instead of drawing it on a surfaceview, first get it working on the imageview
                            //then change to surfaceview to see if it improves performance but imageview should be fine for a first try.
                        }
                    }
                }catch(SocketTimeoutException s)
                {
                    System.out.println("Socket timed out!");
                    break;
                }catch(IOException e)
                {
                    e.printStackTrace();
                    break;
                }
            }
        }

    }
}
