package com.wuyr.bluetoothprinter2.activities;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Toast;

import com.wuyr.bluetoothprinter2.R;
import com.wuyr.bluetoothprinter2.adapter.BluetoothListAdapter;
import com.wuyr.bluetoothprinter2.customize.MySnackBar;
import com.wuyr.bluetoothprinter2.utils.BluetoothUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.wuyr.bluetoothprinter2.R.id.unbind_list;

/**
 * Created by wuyr on 17-7-26 下午1:13.
 */

public class BluetoothListActivity extends AppCompatActivity {

    private BluetoothListAdapter mBondedListAdapter, mUnbindListAdapter;
    private Map<String, BluetoothDevice> mDevices;
    private BluetoothUtil mBluetoothUtil;
    private View mRootView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_blu_list_view);
        init();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mBluetoothUtil.getStatus() == BluetoothUtil.CONNECTING) {
            mBluetoothUtil.setStatus(BluetoothUtil.DISCONNECT);
            mBluetoothUtil.stopFind();
        }
    }

    private void init() {
        initView();
        mDevices = new HashMap<>();
        mBluetoothUtil = BluetoothUtil.getInstance(this);
        Set<BluetoothDevice> tmp = mBluetoothUtil.getBondedDevices();
        for (BluetoothDevice device : tmp) {
            if (checkIsPrinterDevice(device)) {
                String name = getDeviceName(device);
                mBondedListAdapter.add(name);
                mDevices.put(name, device);
            }
        }
        mBluetoothUtil.findDevices(device -> {
            if (checkIsPrinterDevice(device)) {
                String name = getDeviceName(device);
                mDevices.put(name, device);
                if (device.getBondState() == BluetoothDevice.BOND_NONE)
                    mUnbindListAdapter.add(name);
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    mBondedListAdapter.add(name);
                    mUnbindListAdapter.remove(name);
                }
            }
        });
    }

    private boolean checkIsPrinterDevice(BluetoothDevice device) {
        return device.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.Major.IMAGING;
    }

    private void initView() {
        mRootView = findViewById(R.id.root);
        RecyclerView bondedList, unbindList;
        bondedList = (RecyclerView) findViewById(R.id.bonded_list);
        unbindList = (RecyclerView) findViewById(unbind_list);
        mBondedListAdapter = new BluetoothListAdapter(this, R.layout.adapter_blu_list_item_view);
        mUnbindListAdapter = new BluetoothListAdapter(this, R.layout.adapter_blu_list_item_view);
        bondedList.setLayoutManager(new LinearLayoutManager(this));
        unbindList.setLayoutManager(new LinearLayoutManager(this));
        bondedList.setAdapter(mBondedListAdapter);
        unbindList.setAdapter(mUnbindListAdapter);
        mBondedListAdapter.setOnItemClickListener(deviceName -> {
            mBluetoothUtil.stopFind();
            if (mBluetoothUtil.getStatus() != BluetoothUtil.CONNECTING)
                tryConnect(mDevices.get(deviceName));
        });
        mUnbindListAdapter.setOnItemClickListener(deviceName -> {
            MySnackBar.show(mRootView, "正在尝试配对设备，请稍等。。。", Snackbar.LENGTH_LONG);
            mBluetoothUtil.stopFind();
            pairDevice(mDevices.get(deviceName));
        });
    }

    private String getDeviceName(BluetoothDevice device) {
        return device.getName() + " (" + device.getAddress() + ")";
    }

    private void pairDevice(BluetoothDevice device) {
        if (!mBluetoothUtil.pairDevice(device))
            Toast.makeText(this, "配对失败！", Toast.LENGTH_SHORT).show();
    }

    private synchronized void tryConnect(BluetoothDevice device) {
        if (mBluetoothUtil.getStatus() == BluetoothUtil.DISCONNECT) {
            if (device.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.Major.IMAGING) {
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    pairDevice(device);
                }
                MySnackBar.show(mRootView, "正在连接中。。。", Snackbar.LENGTH_LONG);
                mBluetoothUtil.tryConnect(device, new BluetoothUtil.OnConnectListener() {
                    @Override
                    public void successful(BluetoothSocket socket) {
                        MainActivity.mConnectListener.successful(socket);
                        finish();
                    }

                    @Override
                    public void failure(Exception e) {
                        MainActivity.mConnectListener.failure(e);
                        finish();
                    }
                });
            }
        }
    }

}
