package com.clarss.arblue;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import io.ghyeok.stickyswitch.widget.StickySwitch;

public class SplashActivity extends AppCompatActivity {

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;

    Handler bluetoothIn;
    final int handlerState = 0;

    private static final String address = "98:D3:33:80:9E:65";
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private String stsA = "LEFT";
    private String stsB = "LEFT";

    private ConnectedThread mConnectedThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        ImageView imgSplash = (ImageView) findViewById(R.id.imgSplash);
        imgSplash.setVisibility(View.VISIBLE);


        StickySwitch switchAdelnte = (StickySwitch) findViewById(R.id.switch_adelante);
        StickySwitch switchAtras= (StickySwitch) findViewById(R.id.switch_atras);


        switchAdelnte.setOnSelectedChangeListener(new StickySwitch.OnSelectedChangeListener() {
            @Override
            public void onSelectedChange(@NotNull StickySwitch.Direction direction, @NotNull String text) {
                btAdapter = BluetoothAdapter.getDefaultAdapter();
                checkBTState();

                BluetoothDevice device = btAdapter.getRemoteDevice(address);
                createBluetoothSocket(device);

                mConnectedThread = new ConnectedThread(btSocket);
                mConnectedThread.start();

                stsA = direction.name();
                if(stsA.equals("LEFT")) mConnectedThread.write("A");
                else mConnectedThread.write("1");

                try {
                    btSocket.close();
                }
                catch (IOException e2) {
                    Log.d("", e2.toString());
                }
            }
        });

        switchAtras.setOnSelectedChangeListener(new StickySwitch.OnSelectedChangeListener() {
            @Override
            public void onSelectedChange(@NotNull StickySwitch.Direction direction, @NotNull String text) {
                btAdapter = BluetoothAdapter.getDefaultAdapter();
                checkBTState();

                BluetoothDevice device = btAdapter.getRemoteDevice(address);
                createBluetoothSocket(device);

                mConnectedThread = new ConnectedThread(btSocket);
                mConnectedThread.start();

                stsB = direction.name();
                if(stsB.equals("LEFT")) mConnectedThread.write("B");
                else mConnectedThread.write("2");

                try {
                    btSocket.close();
                }
                catch (IOException e2) {
                    Log.d("", e2.toString());
                }
            }
        });

    }


    @Override
    public void onPause()
    {
        super.onPause();
        try {
            btSocket.close();
        }
        catch (IOException e2) {
            Log.d("", e2.toString());
        }
    }

    private void createBluetoothSocket(BluetoothDevice device) {
        try {
            btSocket = device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        } catch (Exception e) {
            Log.e("", "Error creating socket");
        }
        try {
            btSocket.connect();
            Log.e("", "Connected");
        } catch (IOException e) {
            Log.e("", e.getMessage());
            try {
                Log.e("", "trying fallback...");

                btSocket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(device, 1);
                btSocket.connect();

                Log.e("", "Connected");
            } catch (Exception a) {
                Log.e("", a.toString());
            }
        }
    }

    private void checkBTState() {

        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "El dispositivo no soporta bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (!btAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.d("", e.toString());
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }


        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        void write(String input) {
            byte[] msgBuffer = input.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Toast.makeText(getBaseContext(), "La Conexión fallo", Toast.LENGTH_LONG).show();
            }
        }
    }

}
