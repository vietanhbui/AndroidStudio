package com.example.anhbui.enablebluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private final int REQUEST_CODE_ENABLE = 1;
    TextView mTextView;
    ListView mListView;
    BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = findViewById(R.id.text_view);
        mListView = findViewById(R.id.list_view);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        String[] strings = new String[devices.size()];
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, strings);
        mListView.setAdapter(arrayAdapter);

        if (mBluetoothAdapter == null) {
            mTextView.setText("This device does not support bluetooth");
        } else {
            mTextView.setText("This device support bluetooth");
            if (!mBluetoothAdapter.isEnabled()) {
                Intent intentEnableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intentEnableBluetooth, REQUEST_CODE_ENABLE);
            }
        }

        if (devices.size() > 0) {
            int index = 0;
            for (BluetoothDevice device : devices) {
                strings[index++] = device.getName();
            }
            arrayAdapter.notifyDataSetChanged();
        } else {
            mTextView.append(".List pair is none");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ENABLE) {
            if (resultCode == RESULT_OK) {
                mTextView.setText("Bluetooth is enabled");
            } else {
                mTextView.setText("Bluetooth enable is cancelled");
            }
        }
    }
}
