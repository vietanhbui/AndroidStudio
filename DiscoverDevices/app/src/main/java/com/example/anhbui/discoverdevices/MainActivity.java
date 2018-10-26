package com.example.anhbui.discoverdevices;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_CODE_ENABLE_BLUETOOTH = 1;
    private final int REQUEST_CODE_PERMISSION = 2;

    Button mButtonScan;
    ListView mListViewDiscoverDevice;
    ArrayList<String> nameDiscoverDevices;
    ArrayAdapter<String> arrayAdapter;
    BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        addControl();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "This device does not support bluetooth!", Toast.LENGTH_SHORT).show();
        } else {
            addButtonEvent();
            registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        nameDiscoverDevices.add(device.getName());
                        arrayAdapter.notifyDataSetChanged();
                    }
                }
            }, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        }
    }

    private void addButtonEvent() {
        mButtonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBluetoothAdapter == null) {
                    Toast.makeText(MainActivity.this, "This device does not support bluetooth!", Toast.LENGTH_SHORT).show();
                } else {
                    if (!mBluetoothAdapter.isEnabled()) {
                        Intent intentEnableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(intentEnableBluetooth, REQUEST_CODE_ENABLE_BLUETOOTH);
                    }
                    nameDiscoverDevices.clear();
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{"android.permission.ACCESS_COARSE_LOCATION"}, REQUEST_CODE_PERMISSION);
                    mBluetoothAdapter.startDiscovery();
                }
            }
        });
    }

    private void addControl() {
        mButtonScan = findViewById(R.id.button_scan);
        mListViewDiscoverDevice = findViewById(R.id.list_view_discover_device);
        nameDiscoverDevices = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, nameDiscoverDevices);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mListViewDiscoverDevice.setAdapter(arrayAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth is enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bluetooth is cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
