package com.wuyr.bluetoothprinter2.activities;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.wuyr.bluetoothprinter2.R;
import com.wuyr.bluetoothprinter2.customize.MySnackBar;
import com.wuyr.bluetoothprinter2.utils.BluetoothUtil;

import java.io.UnsupportedEncodingException;

/**
 * Created by wuyr on 17-7-15 下午5:31.
 */

public class MainActivity extends AppCompatActivity {

    public static final int OPEN_BLUETOOTH = 1, CHOOSE_IMAGE = 2;
    private BluetoothUtil mBluetoothUtil;
    public static BluetoothUtil.OnConnectListener mConnectListener;
    private BluetoothSocket mSocket;
    private View mRootView;
    private ImageView mConnectStatusView;
    private TextView mConnectStatusText;
    private boolean isActivityDestoryed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_main_view);
        init();
    }

    // TODO: 17-7-16 compress bitmap
    private void init() {
        initView();
        mBluetoothUtil = BluetoothUtil.getInstance(this);
        mConnectListener = new BluetoothUtil.OnConnectListener() {
            @Override
            public void successful(BluetoothSocket socket) {
                mRootView.post(() -> {
                    MySnackBar.show(mRootView, "连接成功！可以愉快地打印了", Snackbar.LENGTH_LONG);
                    mConnectStatusText.setText("打印机已连接");
                    mConnectStatusView.setImageResource(R.drawable.connected);
                });
                mSocket = socket;
            }

            @Override
            public void failure(Exception e) {
                e.printStackTrace();
                if (!isActivityDestoryed) {
                    mRootView.post(() -> {
                        MySnackBar.show(mRootView, "连接蓝牙打印机失败，请重试", Snackbar.LENGTH_LONG);
                        mConnectStatusText.setText("打印机未连接");
                    });
                    mBluetoothUtil.setStatus(BluetoothUtil.DISCONNECT);
                }
            }
        };
    }

    private void initView() {
        mRootView = findViewById(R.id.root);
        mConnectStatusView = (ImageView) findViewById(R.id.ic_connect_status);
        mConnectStatusText = (TextView) findViewById(R.id.connect_status_text);
    }
/*
    public synchronized void tryConnect(BluetoothDevice device) {
        LogUtil.print(device.getName());
        if (mBluetoothUtil.getStatus() == BluetoothUtil.DISCONNECT) {
            if (device.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.Major.IMAGING) {
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    mBluetoothUtil.pairDevice(device);
                }
                mConnectStatusText.setText("正在连接中。。。");
                mBluetoothUtil.tryConnect(device, mConnectListener);
            }
        }
    }

    private String getDeviceName(BluetoothDevice device) {
        return device.getName() + " (" + device.getAddress() + ")";
    }*/

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActivityDestoryed = true;
        if (mBluetoothUtil != null) {
            mBluetoothUtil.stopFind();
            mBluetoothUtil.release();
            mBluetoothUtil = null;
        }
        if (mConnectListener != null)
            mConnectListener = null;
        android.os.Process.killProcess(Process.myPid());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OPEN_BLUETOOTH) {
            if (resultCode == 300)
                startActivity(new Intent(this, BluetoothListActivity.class));
            else MySnackBar.show(mRootView, "请同意打开蓝牙！", Snackbar.LENGTH_SHORT);
        } else if (requestCode == CHOOSE_IMAGE) {
            if (resultCode == RESULT_OK && data != null) {
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                String path = "";
                if (cursor != null) {
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    path = cursor.getString(columnIndex);
                    cursor.close();
                }
                if (!path.isEmpty()) {
                    printImage(path);
                }
            } else {
                MySnackBar.show(mRootView, "加载图片失败。", Snackbar.LENGTH_SHORT);
            }
        }
    }

    public void connectPrinter(View view) {
        if (!mBluetoothUtil.isBluetoothOpen()) {
            if ((mSocket == null || !mSocket.isConnected()) && mBluetoothUtil.getStatus() == BluetoothUtil.DISCONNECT)
                mBluetoothUtil.openBluetooth();
        } else startActivity(new Intent(this, BluetoothListActivity.class));
        /*if ((mSocket == null || !mSocket.isConnected()) && mBluetoothUtil.getStatus() == BluetoothUtil.DISCONNECT) {
            mBluetoothUtil.openBluetooth();
            refreshBondedDevices();
            mBluetoothUtil.findDevices(this::tryConnect);
            MySnackBar.show(mRootView, "正在搜索并连接蓝牙打印机，请稍等片刻", Snackbar.LENGTH_INDEFINITE);
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    MainActivity.this.runOnUiThread(() -> Toast.makeText(MainActivity.this, "如果长时间未连接到设备，请到蓝牙设置里面手动配对", Toast.LENGTH_LONG).show());
                }
            }, 10000, 10000);
        }*/
    }

    public void printImage(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            verifyStoragePermissions(this);
        if (checkIsConnected()) {
            try {
                startActivityForResult(new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI), CHOOSE_IMAGE);
            } catch (Exception e) {
                e.printStackTrace();
                MySnackBar.show(mRootView, "打开图库失败", Snackbar.LENGTH_LONG);
            }
        }
    }

    private void printImage(String path) {
        if (checkIsConnected()) {
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(path);
                mBluetoothUtil.printBitmap(bitmap);
                mBluetoothUtil.feed();
                mBluetoothUtil.feed();
                mBluetoothUtil.writeData(mSocket);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void printBill(View view) {
        if (checkIsConnected()) {
            BluetoothUtil.PrintParams printParams = new BluetoothUtil.PrintParams();

            printParams.setWidth2x(1);
            printParams.setHeight2x(1);
            mBluetoothUtil.printFormatText(formatCenter("★★XX美食店★★", isWidth2x(printParams)), printParams);
            mBluetoothUtil.feed();
            mBluetoothUtil.printFormatText(formatCenter("★在线支付订单★", isWidth2x(printParams)), printParams);
            mBluetoothUtil.feed();
            mBluetoothUtil.feed();
            printParams.setHeight2x(0);
            printParams.setWidth2x(0);
            mBluetoothUtil.printFormatText("X X美食店 XXX分店）\n" +
                    "XX市XX区XXXXXXXXXXXXXXXX\n" +
                    "电话：12345678901\n" +
                    "官网：www.xxx.com", printParams);
            mBluetoothUtil.feed();

            mBluetoothUtil.printFullLine("-", isWidth2x(printParams));
            mBluetoothUtil.printFormatText("下单时间：2017-07-28 23:18:22\n\n", printParams);
            mBluetoothUtil.printFullLine("-", isWidth2x(printParams));
            mBluetoothUtil.feed();

            mBluetoothUtil.printFormatText(handleString("美食篮子/%/数量/%/单价/%/金额\n", isWidth2x(printParams)), printParams);
            mBluetoothUtil.printFullLine("-", isWidth2x(printParams));
            mBluetoothUtil.printFormatText(handleString("麻婆豆腐/%/1/%/10/%/10\n", isWidth2x(printParams)), printParams);
            mBluetoothUtil.printFormatText(handleString("土豆炒肉/%/1/%/12/%/12\n", isWidth2x(printParams)), printParams);
            mBluetoothUtil.printFormatText(handleString("黄焖鸡米/%/1/%/15/%/15\n", isWidth2x(printParams)), printParams);
            mBluetoothUtil.printFormatText(handleString("海参猪脚汤/%/1/%/9/%/9\n", isWidth2x(printParams)), printParams);
            mBluetoothUtil.printFormatText(handleString("米饭/%/2/%/2/%/4\n", isWidth2x(printParams)), printParams);
            mBluetoothUtil.printFullLine("-", isWidth2x(printParams));
            mBluetoothUtil.printFormatText(handleString("小计/%/1/%/--/%/30\n", isWidth2x(printParams)), printParams);
            mBluetoothUtil.printFormatText(handleString("优惠折扣/%/1/%/-20/%/-20\n", isWidth2x(printParams)), printParams);

            mBluetoothUtil.printFullLine("-", isWidth2x(printParams));
            printParams.setHeight2x(1);
            printParams.setWidth2x(1);
            mBluetoothUtil.printFormatText(handleString("合计：30（已付款）\n", isWidth2x(printParams)), printParams);
            printParams.setHeight2x(0);
            printParams.setWidth2x(0);
            mBluetoothUtil.printFormatText("", printParams);
            mBluetoothUtil.printFullLine("-", isWidth2x(printParams));

            printParams.setHeight2x(1);
            printParams.setWidth2x(1);
            mBluetoothUtil.printFormatText("备注：不加辣", printParams);
            mBluetoothUtil.feed();
            mBluetoothUtil.feed();
            mBluetoothUtil.feed();
            mBluetoothUtil.writeData(mSocket);
        }
    }

    private boolean isWidth2x(BluetoothUtil.PrintParams printParams) {
        return printParams.getWidth2x() == 1;
    }

    public int getKeyCount(String str, String key) {
        if (str == null || key == null || "".equals(str.trim()) || "".equals(key.trim())) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = str.indexOf(key, index)) != -1) {
            index += key.length();
            count++;
        }
        return count;
    }

    private String handleString(String src, boolean isWidth2x) {
        String[] items = src.split("/%/");
        int totalLength = isWidth2x ? 24 : 48;
        int stringLength = 0;
        int stringCount = 0;
        for (String tmp : items) {
            if (!tmp.isEmpty()) {
                stringLength += getStringRealLength(tmp)/2;
                stringCount++;
            }
        }

        if (stringCount - 1 < 1)
            return src;
        int spaceLength = (totalLength - stringLength) / (stringCount - 1);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < stringCount; i++) {
            result.append(items[i]);
            if (i == stringCount - 1) break;
            int spaceCount = Math.abs(spaceLength - getStringRealLength(items[i]));
            for (int i2 = 0; i2 < spaceCount; i2++)
                result.append(" ");
        }
        return result.toString();
    }

    private int getStringRealLength(String src) {
        char[] items = src.toCharArray();
        int chineseCharCount = 0;
        try {
            for (char c : items) {
                if (new String(new char[]{c}).getBytes("GBK").length == 2)
                    chineseCharCount++;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        int result = src.length();
        result -= chineseCharCount;
        result = result + (chineseCharCount * 2);
        return result;
    }


    private String formatCenter(String src, boolean isWidth2x) {
        int totalLength = isWidth2x ? 24 : 48;
        int stringLength = src.length();
        // TODO: 17-7-28 chinese use 2 byte
        int spaceCount = (totalLength / (isWidth2x ? 4 : 2)) - (stringLength / (isWidth2x ? 4 : 2));
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < spaceCount; i++)
            result.append(" ");
        result.append(src);
        return result.toString();
    }

    private Bitmap getBitmapFromAssets(String fileName) {
        try {
            return BitmapFactory.decodeStream(getResources().getAssets().open(fileName));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void printOrder(View view) {
        if (checkIsConnected()) {
            try {
                Bitmap bitmap = getBitmapFromAssets("p3.jpg");
                if (bitmap != null)
                    mBluetoothUtil.printBitmap(bitmap);
                mBluetoothUtil.feed();
                mBluetoothUtil.feed();
                mBluetoothUtil.writeData(mSocket);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //noinspection StatementWithEmptyBody
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            /*if (checkIsConnected()) {
                try {
                    startActivityForResult(new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI), CHOOSE_IMAGE);
                } catch (Exception e)
                    e.printStackTrace();
                    MySnackBar.show(mRootView, "打开图库失败", Snackbar.LENGTH_LONG);
                }
            }*/
        } else {
            MySnackBar.show(mRootView, "抱歉，我无法获取内存卡读取权限", Snackbar.LENGTH_LONG);
        }

    }

    private boolean checkIsConnected() {
        boolean result = isConnected();
        if (!result)
            MySnackBar.show(mRootView, "打印机未连接！", Snackbar.LENGTH_SHORT);
        return result;
    }

    private boolean isConnected() {
        return mSocket != null && mSocket.isConnected();
    }

    public void disconnect(View view) {
        checkIsConnected();
        try {
            if (mSocket != null)
                mSocket.close();
            mSocket = null;
            mBluetoothUtil.setStatus(BluetoothUtil.DISCONNECT);
            mConnectStatusText.setText("打印机未连接");
            mConnectStatusView.setImageResource(R.drawable.disconnect);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void feed(View view) {
        checkIsConnected();
        mBluetoothUtil.feed();
        mBluetoothUtil.writeData(mSocket);
    }

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    public static void verifyStoragePermissions(Activity activity) {
// Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
// We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }

    public void printCode(View view) {
        if (checkIsConnected()) {
            EditText editText = new EditText(this);
            editText.setSingleLine(true);
            editText.setMaxLines(1);
            editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(23)});
            new AlertDialog.Builder(this).setCancelable(false).setView(editText).setTitle("请输入条形码内容(最长23字符)").setNegativeButton("取消", null).setPositiveButton("确定", (dialogInterface, i) -> {
                if (editText.getText() != null && !editText.getText().toString().isEmpty()) {
                    printCode(editText.getText().toString());
                } else MySnackBar.show(mRootView, "内容为空！", Snackbar.LENGTH_SHORT);
            }).show();
        }
    }

    public void printQRCode(View view) {
        if (checkIsConnected()) {
            EditText editText = new EditText(this);
            editText.setSingleLine(true);
            editText.setMaxLines(1);
            new AlertDialog.Builder(this).setCancelable(false).setView(editText).setTitle("请输入二维码码内容").setNegativeButton("取消", null).setPositiveButton("确定", (dialogInterface, i) -> {
                if (editText.getText() != null && !editText.getText().toString().isEmpty()) {
                    printQRCode(editText.getText().toString());
                } else MySnackBar.show(mRootView, "内容为空！", Snackbar.LENGTH_SHORT);
            }).show();
        }
    }


    private void printCode(String content) {
        mBluetoothUtil.printCode(content);
        mBluetoothUtil.feed();
        mBluetoothUtil.feed();
        mBluetoothUtil.writeData(mSocket);
    }

    private void printQRCode(String content) {
        mBluetoothUtil.printQRCode(content);
        mBluetoothUtil.feed();
        mBluetoothUtil.feed();
        mBluetoothUtil.writeData(mSocket);
    }
}
