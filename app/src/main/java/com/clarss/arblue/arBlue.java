package com.clarss.arblue;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class arBlue extends AppWidgetProvider {

    private static final String ENCENDER = "encender";
    private static final String APAGAR = "apagar";


    Handler bluetoothIn;
    final int handlerState = 0;

    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private static final String address = "98:D3:33:80:9E:65";
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private ConnectedThread mConnectedThread;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (APAGAR.equals(intent.getAction())) apagar();
        else if (ENCENDER.equals(intent.getAction())) encender();
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.ar_blue);

        views.setImageViewResource(R.id.imgAp, R.mipmap.off);
        views.setImageViewResource(R.id.imgConnect, R.mipmap.icono);
        views.setImageViewResource(R.id.imgEn, R.mipmap.on);

        views.setOnClickPendingIntent(R.id.imgEn, getPendingSelfIntent(context, ENCENDER));
        views.setOnClickPendingIntent(R.id.imgAp, getPendingSelfIntent(context, APAGAR));

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);


    }

    protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, arBlue.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
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

    public void encender() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            if (btAdapter.isEnabled()) {

                BluetoothDevice device = btAdapter.getRemoteDevice(address);
                createBluetoothSocket(device);

                mConnectedThread = new ConnectedThread(btSocket);
                mConnectedThread.start();

                mConnectedThread.write("1");
                mConnectedThread.write("2");

                try {
                    btSocket.close();
                } catch (IOException e2) {
                    Log.d("", e2.toString());
                }
            }
        }
    }

    public void apagar(){
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btAdapter!=null) {
            if (btAdapter.isEnabled()) {
                BluetoothDevice device = btAdapter.getRemoteDevice(address);
                createBluetoothSocket(device);

                mConnectedThread = new ConnectedThread(btSocket);
                mConnectedThread.start();

                mConnectedThread.write("A");
                mConnectedThread.write("B");

                try {
                    btSocket.close();
                }
                catch (IOException e2) {
                    Log.d("", e2.toString());
                }
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
                //Toast.makeText(getBaseContext(), "La Conexión fallo", Toast.LENGTH_LONG).show();
            }
        }
    }
}

