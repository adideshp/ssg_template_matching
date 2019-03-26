package com.example.myapplication;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.example.myapplication.MyBluetoothService;


public class DeviceDetailActivity extends AppCompatActivity {

    public static String EXTRA_ADDRESS = "device_address";
    public static String DEVICE_INFO = "device_info";
    public String selectedAddress = null;
    public String deviceInfo = null;
    private BluetoothAdapter myBluetooth = null;
    private ProgressDialog progress;
    private ProgressDialog inputReceiveProgress;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    Button viewTemplateBtn, createTemplateBtn, saveTemplate, cancel;
    TextView commands;
    ImageView template;

    private static final String TAG = "MY_APP_DEBUG_TAG";
    private Handler handler; // handler that gets info from Bluetooth service

    // Defines several constants used when transmitting messages between the
    // service and the UI.

    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

        // ... (Add other message types here as needed.)
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        MyBluetoothService ioService = new MyBluetoothService();

        viewTemplateBtn = (Button) findViewById(R.id.viewTemplate);
        createTemplateBtn = (Button) findViewById(R.id.createTemplate);
        template = (ImageView)  findViewById(R.id.templateView);
        saveTemplate = (Button) findViewById(R.id.templateSave);
        cancel  = (Button) findViewById(R.id.cancel);

        saveTemplate.setEnabled(false);
        cancel.setEnabled(false);

        Intent newint = getIntent();
        selectedAddress = newint.getStringExtra(MainActivity.EXTRA_ADDRESS);
        deviceInfo = newint.getStringExtra(MainActivity.DEVICE_INFO);

        final String CMD_VIEW_TEMPLATE = "1\n";
        final String CMD_TAKE_SNAP = "2\n";
        final String CMD_SAVE_LAST_SNAP = "3\n";

        new ConnectBT().execute();


        viewTemplateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                Snackbar.make(v, "You will see the template shortly", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                sendCommand(CMD_VIEW_TEMPLATE);
                receiveImage();
            }
        });

        createTemplateBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                viewTemplateBtn.setEnabled(false);
                Snackbar.make(v, "You will see the click shortly", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                saveTemplate.setEnabled(true);
                cancel.setEnabled(true);
                sendCommand(CMD_TAKE_SNAP);
                receiveImage();
                //onConnect();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                template.setImageDrawable(null);
                saveTemplate.setEnabled(false);
                cancel.setEnabled(false);
                viewTemplateBtn.setEnabled(true);
            }
        });

        saveTemplate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //send a request to use the current image as a template for template matching
                Snackbar.make(v, "Last image saved as a template", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                sendCommand(CMD_SAVE_LAST_SNAP);
                saveTemplate.setEnabled(false);
                cancel.setEnabled(false);
                viewTemplateBtn.setEnabled(true);
            }
        });

        getSupportActionBar().setTitle("Device Details");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void onConnect() {
        Intent intent = new Intent(this, ClickTemplateActivity.class);
        startActivity(intent);
    }


    private void sendCommand( String number ) {
        if ( btSocket != null ) {
            try {
                btSocket.getOutputStream().write(number.getBytes());
            } catch (IOException e) {
                msg("Error");
            }
        }
    }

    private void receiveImage() {
        try {
            InputStream is = btSocket.getInputStream();
            Bitmap output = readInputStreamWithTimeout(is, 5000);
            template.setImageBitmap(output);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Bitmap readInputStreamWithTimeout(InputStream is, int timeoutMillis)
            throws IOException  {
        int bufferOffset = 0;
        byte[] image = new byte[20000];

        String output = "";
        int ptr = 0;
        long maxTimeMillis = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < maxTimeMillis) {
            int readLength = is.available();
            int res = is.read(image, ptr, readLength);
            ptr = ptr + readLength;
        }
        return BitmapFactory.decodeByteArray(image, 0, ptr);
    }

    /*

    public String readInputStreamWithTimeout(InputStream is, int timeoutMillis)
            throws IOException  {
        int bufferOffset = 0;
        byte[] image = new byte[20000];

        String output = "";
        int ptr = 0;
        long maxTimeMillis = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < maxTimeMillis) {
            int readLength = is.available();
            int res = is.read(image, ptr, readLength);
            ptr = ptr + readLength;
        }

        return new String(image);
    }
    */

    private void Disconnect () {
        if ( btSocket!=null ) {
            try {
                btSocket.close();
            } catch(IOException e) {
                msg("Error");
            }
        }

        finish();
    }

    private void msg (String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }


    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean ConnectSuccess = true;

        @Override
        protected  void onPreExecute () {
            progress = ProgressDialog.show(DeviceDetailActivity.this, "Connecting...", "Please Wait!!!");
        }

        @Override
        protected Void doInBackground (Void... devices) {
            try {
                if ( btSocket==null || !isBtConnected ) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(selectedAddress);
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                }
            } catch (IOException e) {
                ConnectSuccess = false;
            }

            return null;
        }

        @Override
        protected void onPostExecute (Void result) {
            super.onPostExecute(result);
            progress.dismiss();
            if (!ConnectSuccess) {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            } else {
                msg("Connected");
                isBtConnected = true;
                TextView deviceDetail = findViewById(R.id.deviceDetail);
                deviceDetail.setText(deviceInfo);
                //ioThread = new ConnectedThread(btSocket);
                //ioThread.run();  // Starts the thread
            }
        }
    }
}
