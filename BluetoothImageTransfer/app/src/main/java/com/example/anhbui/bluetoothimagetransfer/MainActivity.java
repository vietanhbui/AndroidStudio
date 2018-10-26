package com.example.anhbui.bluetoothimagetransfer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    Button mButtonListen, mButtonListDevices, mButtonSend;
    ListView mListView;
    TextView mTextViewStatus;
    ImageView mImageView;

    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice[] mBluetoothDevices;
    SendReceive sendReceive;

    private static final int STATE_LISTENING = 1;
    private static final int STATE_CONNECTING = 2;
    private static final int STATE_CONNECTED = 3;
    private static final int STATE_CONNECTION_FAILED = 4;
    private static final int STATE_MESSAGE_RECEIVED = 5;

    private final int REQUEST_ENABLE_BLUETOOTH = 6;

    private static final String APP_NAME = "BT Chat";
    private static final UUID MY_UUID = UUID.fromString("18e53fd2-1d02-4ba9-9af5-cd706a770779");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        addControl();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intentEnableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intentEnableBluetooth, REQUEST_ENABLE_BLUETOOTH);
        }
        addEvents();
    }

    private void addEvents() {
        mButtonListDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
                String[] strings = new String[devices.size()];
                mBluetoothDevices = new BluetoothDevice[devices.size()];
                int index = 0;
                if (devices.size() > 0) {
                    for (BluetoothDevice device : devices) {
                        mBluetoothDevices[index] = device;
                        strings[index++] = device.getName();
                    }
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, strings);
                    mListView.setAdapter(arrayAdapter);
                }
            }
        });
        mButtonListen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ServerClass serverClass = new ServerClass();
                serverClass.start();
            }
        });
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ClientClass clientClass = new ClientClass(mBluetoothDevices[position]);
                clientClass.start();
                mTextViewStatus.setText("Connecting");
            }
        });
        mButtonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.happyicon);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] imageBytes = stream.toByteArray();
                int subArraySize = 400;
                sendReceive.write(String.valueOf(imageBytes.length).getBytes());
                for (int i = 0; i < imageBytes.length; i += subArraySize) {
                    byte[] tempArray = Arrays.copyOfRange(imageBytes, i, Math.min(imageBytes.length, i + subArraySize));
                    sendReceive.write(tempArray);
                }
            }
        });
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case STATE_LISTENING:
                    mTextViewStatus.setText("Listening");
                    break;
                case STATE_CONNECTED:
                    mTextViewStatus.setText("Connected");
                    break;
                case STATE_CONNECTING:
                    mTextViewStatus.setText("Connecting");
                    break;
                case STATE_CONNECTION_FAILED:
                    mTextViewStatus.setText("Connection failed");
                    break;
                case STATE_MESSAGE_RECEIVED:
                    byte[] readBuff = (byte[]) msg.obj;
                    Bitmap bitmap = BitmapFactory.decodeByteArray(readBuff, 0, msg.arg1);
                    mImageView.setImageBitmap(bitmap);
                    break;
            }
            return true;
        }
    });

    private void addControl() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mButtonListDevices = findViewById(R.id.button_list_devices);
        mButtonListen = findViewById(R.id.button_listen);
        mButtonSend = findViewById(R.id.button_send);
        mListView = findViewById(R.id.list_view);
        mTextViewStatus = findViewById(R.id.text_view_status);
        mImageView = findViewById(R.id.image_view);
    }

    private class ServerClass extends Thread {
        private BluetoothServerSocket mBluetoothServerSocket;

        public ServerClass() {
            try {
                mBluetoothServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            BluetoothSocket mBluetoothSocket = null;
            while (mBluetoothSocket == null) {
                try {
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTING;
                    handler.sendMessage(message);
                    mBluetoothSocket = mBluetoothServerSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }
                if (mBluetoothSocket != null) {
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTED;
                    handler.sendMessage(message);
                    sendReceive = new SendReceive(mBluetoothSocket);
                    sendReceive.start();
                    break;
                }
            }
        }
    }

    private class ClientClass extends Thread {
        private BluetoothSocket mBluetoothSocket;
        private BluetoothDevice mBluetoothDevice;

        public ClientClass(BluetoothDevice device) {
            mBluetoothDevice = device;
            try {
                mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            super.run();
            try {
                mBluetoothSocket.connect();
                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);
                sendReceive = new SendReceive(mBluetoothSocket);
                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }
    }

    private class SendReceive extends Thread {
        private final BluetoothSocket mBluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive(BluetoothSocket socket) {
            mBluetoothSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;
            try {
                tempIn = mBluetoothSocket.getInputStream();
                tempOut = mBluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = tempIn;
            outputStream = tempOut;
        }

        @Override
        public void run() {
            super.run();
            byte[] buffer = null;
            int numberOfBytes = 0;
            int index = 0;
            boolean flag = true;
            while (true) {
                if (flag) {
                    try {
                        byte[] temp = new byte[inputStream.available()];
                        if (inputStream.read(temp) > 0) {
                            numberOfBytes = Integer.parseInt(new String(temp, "UTF-8"));
                            buffer = new byte[numberOfBytes];
                            flag = false;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        byte[] data = new byte[inputStream.available()];
                        int numbers = inputStream.read(data);
                        System.arraycopy(data, 0, buffer, index, numbers);
                        index = index + numbers;
                        if (index == numberOfBytes) {
                            handler.obtainMessage(STATE_MESSAGE_RECEIVED, numberOfBytes, -1, buffer).sendToTarget();
                            flag = true;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
