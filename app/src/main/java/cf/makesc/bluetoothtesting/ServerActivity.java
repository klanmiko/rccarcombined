package cf.makesc.bluetoothtesting;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdManager.RegistrationListener;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import scmaker.com.arduinowifi.wrappedlayout;


public class ServerActivity extends Activity{

    private ServerSocket serverSocket;
    public static Bitmap image = null;
    static final int PORT = 56469;
    ImageView imageView;
    long oldtime,newtime,delta;
    long oldtimeb,newtimeb,deltab;
    NsdManager mNsdManager;
    public Handler handler;
    String mServiceName;
    RegistrationListener mRegistrationListener = new RegistrationListener() {

        @Override
        public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
            // Save the service name.  Android may have changed it in order to
            // resolve a conflict, so update the name you initially requested
            // with the name Android actually used.
            mServiceName = NsdServiceInfo.getServiceName();
        }

        @Override
        public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            // Registration failed!  Put debugging code here to determine why.
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo arg0) {
            // Service has been unregistered.  This only happens when you call
            // NsdManager.unregisterService() and pass in this listener.
        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            // Unregistration failed.  Put debugging code here to determine why.
        }
    };
    public void registerService(int port, Context context, InetAddress host) {
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setServiceName("RobotController");
        serviceInfo.setServiceType("_sum._tcp.");
        serviceInfo.setPort(port);
        serviceInfo.setHost(host);
        mNsdManager = (NsdManager)context.getSystemService(Context.NSD_SERVICE);
        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        imageView = (ImageView) findViewById(R.id.cam_image);
        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message inputMessage) {
                newtimeb=System.currentTimeMillis();
                deltab=newtimeb-oldtimeb;
                Log.i("drawfps",String.valueOf(deltab));
                // Gets the image task from the incoming Message object.
                Bitmap bm = (Bitmap)inputMessage.obj;
                imageView.setImageBitmap(bm);
                super.handleMessage(inputMessage);
                oldtimeb=newtimeb;
            }

        };
        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ServerThread serverThread = new ServerThread(this);
        serverThread.start();
        registerService(PORT,this.getApplicationContext(),serverSocket.getInetAddress());
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
                    byte buffer[]={0x00,0x00,0x00};
                    byte start[]={0x48,0x41,0x23};
                    while (server.isConnected()) {
                        if (inStream.available()>3){

                            Log.i("fps",String.valueOf(delta));
                            while(buffer[0]!=start[0]||buffer[1]!=start[1]||buffer[2]!=start[2])
                            {
                                if(inStream.available()>0) {
                                    buffer[0] = buffer[1];
                                    buffer[1] = buffer[2];
                                    buffer[2] = inStream.readByte();
                                }
                                Log.i("parsing","parsing");
                            }
                            int size =  inStream.readInt();
                            buffer[0]=0;
                            buffer[1]=0;
                            buffer[2]=0;
                            //read image size
                            Log.i("stuff","reading");
                            Log.i("size", String.valueOf(size));
                            byte[] imageBytes = new byte[size];
                            int counter=0;
                            inStream.readFully(imageBytes,0,size);

                            Bitmap bm = BitmapFactory.decodeByteArray(imageBytes, 0, size);
                            newtime=System.currentTimeMillis();
                            delta=newtime-oldtime;
                            //Bitmap bm = BitmapFactory.decodeStream(inStream);
                            if(bm!=null) {
                                Message frame = handler.obtainMessage(1, bm);
                                frame.sendToTarget();
                            }
                            oldtime=newtime;
                            Thread.sleep(1);
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
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
