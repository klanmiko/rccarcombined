package cf.makesc.bluetoothtesting;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;


public class ClientActivity extends Activity {

    private Camera mCamera;
    private CameraPreview mPreview;
    public InetAddress host;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if(extras.containsKey("host"))
            {
                host = (InetAddress) extras.get("host");
            }
        }
        setContentView(R.layout.activity_client);

        mCamera = Camera.open(0);
        mPreview = new CameraPreview(this, mCamera, host);
        FrameLayout preview = (FrameLayout) findViewById(R.id.client_preview);
        preview.addView(mPreview);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mCamera.release();
            mCamera = null;
        }
    }
}
