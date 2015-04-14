package scmaker.com.arduinowifi;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import scmaker.com.arduinowifi.serverport.Server;
import scmaker.com.arduinowifi.serverport.SessionProtocol;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * Use the {@link wrappedlayout#newInstance} factory method to
 * create an instance of this fragment.
 */
public class wrappedlayout extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment wrappedlayout.
     */
    // TODO: Rename and change types and number of parameters
    public static wrappedlayout newInstance(String param1, String param2) {
        wrappedlayout fragment = new wrappedlayout();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public wrappedlayout() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_wrappedlayout, container, false);
        SeekBar hori = (SeekBar) v.findViewById(R.id.horizontal);
        hori.setOnSeekBarChangeListener(horizontal);
        VerticalSeekBar verti = (VerticalSeekBar) v.findViewById(R.id.vertical);
        verti.setOnSeekBarChangeListener(vertical);
        aed=new ConnectThread();
        try {
            new NetworkWrapper().start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return v;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public ServerSocket ssock;
    Handler msghandler;
    ConnectThread aed;
    SeekBar.OnSeekBarChangeListener horizontal = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            aed.setSteer((i - 50)*2);
            Server.setSteer((i - 50) * 2);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            seekBar.setProgress(50);
            aed.setSteer(0);
            Server.setSteer(0);
        }

    };
    SeekBar.OnSeekBarChangeListener vertical = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            aed.setSpeed((i - 50)*2);
            Server.setSpeed((i - 50) * 2);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            seekBar.setProgress(50);
            aed.setSpeed(0);
            Server.setSpeed(0);
        }
    };
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }
    public void disconnect(View view) {
        try {
            if(aed.client!=null) {
                aed.client.close();
            }
        }
        catch(IOException e)
        {

        }
    }
    public void sayhi(View view){
        try {
            if(aed.client!=null) {
                aed.hi();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void saybye(View view){

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    public class ClientThread extends Thread implements Runnable {
        public Socket client;
        InputStream reader;
        OutputStream writer;
        private int speed, steer;
        public ClientThread() {
        }

        public void run() {
            InetAddress serveradd = null;
            try {
                serveradd = InetAddress.getByName("10.255.146.24");
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return;
            }
            while (true) {
                Log.i("SUP", "waiting");
                try {
                    client=new Socket(serveradd,6551);
                    reader = client.getInputStream();
                    writer = client.getOutputStream();
                    while (client.isConnected()) {
                        encodeData(getSteer(), getSpeed());
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        }
        public synchronized int getSteer() {
            return steer;
        }

        public synchronized void setSteer(int steer) {
            this.steer = steer;
        }

        public synchronized int getSpeed() {
            return speed;
        }

        public synchronized void setSpeed(int speed) {
            this.speed = speed;
        }
        public void encodeData(int steer,int speed) throws IOException, InterruptedException {
            byte buffer[]={0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
            buffer[0]= SessionProtocol.startbyte;
            buffer[1]=SessionProtocol.steerbyte;
            if(steer<0)
            {
                buffer[2]=0x00;
                buffer[3]=(byte)(steer*-1);
            }
            else if(steer>0){
                buffer[2]=0x01;
                buffer[3]=(byte)steer;
            }
            buffer[4]=SessionProtocol.speedbyte;
            if(speed<0)
            {
                buffer[5]=0x00;
                buffer[6]=(byte)(speed*-1);
            }
            else if(speed>0){
                buffer[5]=0x01;
                buffer[6]=(byte)speed;
            }
            buffer[7]=SessionProtocol.delimbytes;
            writer.write(buffer);
            synchronized (writer) {
                writer.wait(1);
            }
            writer.flush();
        }
        public void hi() throws IOException {
            byte[] asdf={0x01,0x02,0x03};
            writer.write(asdf);
        }
    }
    public void onPause()
    {
        super.onPause();
        aed.interrupt();
    }
    public void onResume()
    {
        super.onResume();
        if(!aed.isAlive()) {
            aed.start();
        }
    }
    public class ConnectThread extends Thread implements Runnable {
        public Socket client;
        InputStream reader;
        OutputStream writer;
        private int speed, steer;

        public ConnectThread() {
        }

        public void run() {
            try {
                ssock = new ServerSocket(6551);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            Log.i("SUP", "threadstarted");
            while (true) {
                Log.i("SUP", "waiting");
                try {
                    client = ssock.accept();
                    reader = client.getInputStream();
                    writer = client.getOutputStream();
                    while (client.isConnected()) {
                        encodeData(getSteer(), getSpeed());
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            try {
                                client.close();
                                ssock.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                            return;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    try {
                        client.close();
                        ssock.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    return;
                }

            }

        }
        public void hi() throws IOException {
            byte[] asdf={0x01,0x02,0x03};
            writer.write(asdf);
        }
        public void onDestroy() {
        }
        public synchronized int getSteer() {
            return steer;
        }

        public synchronized void setSteer(int steer) {
            this.steer = steer;
        }

        public synchronized int getSpeed() {
            return speed;
        }

        public synchronized void setSpeed(int speed) {
            this.speed = speed;
        }
        public void encodeData(int steer,int speed) throws IOException, InterruptedException {
            byte buffer[]={0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};
            buffer[0]= SessionProtocol.startbyte;
            buffer[1]=SessionProtocol.steerbyte;
            Log.i("STEER",String.valueOf(steer));
            Log.i("SPEED",String.valueOf(speed));
            if(steer<0)
            {
                buffer[2]=0x00;
                buffer[3]=(byte)(steer*-1);
            }
            else if(steer>0){
                buffer[2]=0x01;
                buffer[3]=(byte)steer;
            }
            buffer[4]=SessionProtocol.speedbyte;
            if(speed<0)
            {
                buffer[5]=0x00;
                buffer[6]=(byte)(speed*-1);
            }
            else if(speed>0){
                buffer[5]=0x01;
                buffer[6]=(byte)speed;
            }
            buffer[7]=SessionProtocol.delimbytes;
            writer.write(buffer);
            writer.flush();
        }
    }
    public class NetworkWrapper extends Thread implements Runnable{
        public void run()
        {
            try {
                new Server(6552).run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
