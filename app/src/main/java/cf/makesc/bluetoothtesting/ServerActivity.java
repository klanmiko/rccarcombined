package cf.makesc.bluetoothtesting;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        imageView = (ImageView) findViewById(R.id.cam_image);

        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ServerThread serverThread = new ServerThread();
        serverThread.start();

    }

    class ServerThread extends Thread {
        public void run()
        {
            while(true)
            {
                try
                {
                    Log.e("currently","waiting");
                    Socket server = serverSocket.accept(); //should probably switch the sides that do accept()
                    DataInputStream inStream = new DataInputStream(server.getInputStream());
                    Log.e("connected","successful");
                    while (server.isConnected()) {
                        if (inStream.available()>0) {
                            int size =  inStream.readInt(); //read image size
                            byte[] imageBytes = new byte[16384];
                            inStream.read(imageBytes, 0, size); //read a byte array of size size that will be converted to a bitmap
                            imageView.setImageBitmap(BitmapFactory.decodeByteArray(imageBytes,0, size));
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
