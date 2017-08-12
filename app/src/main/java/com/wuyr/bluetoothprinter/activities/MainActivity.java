package com.wuyr.bluetoothprinter.activities;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.wuyr.bluetoothprinter.R;
import com.wuyr.bluetoothprinter.customize.MySnackBar;
import com.wuyr.bluetoothprinter.utils.BluetoothUtil;

import static android.os.Build.VERSION_CODES.M;

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
        initDialog();
    }

    private void initView() {
        mRootView = findViewById(R.id.root);
        mConnectStatusView = (ImageView) findViewById(R.id.ic_connect_status);
        mConnectStatusText = (TextView) findViewById(R.id.connect_status_text);
    }

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
    }

    public void printImage(View view) {
        if (Build.VERSION.SDK_INT >= M)
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

    private AlertDialog mDialog;
    private EditText startStation, startStationPhone, endStation, endStationPhone, receiveAddress, sender, num, weight,
            name, receiver, phone, collection, startTime;

    private void initDialog() {
        View contentView = LayoutInflater.from(this).inflate(R.layout.dialog_init_order_view, null, false);
        initDialogViews(contentView);
        mDialog = new AlertDialog.Builder(this).setNegativeButton("取消", null).setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (checkFill()) {
                    printHeadPic("2017081500001");
                    String content = "\n发站: %s到站: %s\n\n" +
                            "发站电话: %s到站电话: %s\n\n" +
                            "收件地址: %s\n\n" +
                            "发件人: %s件数: %s重量: %sKG\n\n" +
                            "货物名称: %s配送: %s\n\n" +
                            "收件人: %s电话: %s\n\n" +
                            "付款: %s代收: %s\n\n" +
                            "发车时间: %s车牌号:\n\n" +
                            "到达时间: 次日达\n\n";
                    mBluetoothUtil.printFormatText(handleString(content), null);
                    mBluetoothUtil.feed();
                    mBluetoothUtil.feed();
                    mBluetoothUtil.feed();
                    mBluetoothUtil.writeData(mSocket);
                } else
                    MySnackBar.show(mRootView, "失败！请先填写好列表", Snackbar.LENGTH_LONG);
            }

            private void printHeadPic(String orderNumber) {
                try {
                    Bitmap bitmap = Bitmap.createBitmap(2000, 580, Bitmap.Config.RGB_565);
                    Bitmap barCodeBitmap = mBluetoothUtil.getBarCodeBitmap(orderNumber);
                    Canvas canvas = new Canvas(bitmap);
                    Paint paint = new Paint();
                    paint.setAntiAlias(true);
                    canvas.drawARGB(255, 255, 255, 255);
                    canvas.drawBitmap(barCodeBitmap, 940, 30, paint);
                    paint.setTextSize(150);
                    canvas.drawText("XXX快递", 138, 150, paint);
                    paint.setTextSize(85);
                    canvas.drawText("方便 智慧 普惠", 165, 420, paint);
                    canvas.drawText("诚信 绿色 快递", 165, 540, paint);
                    char[] numbers = orderNumber.toCharArray();
                    StringBuilder builder = new StringBuilder();
                    for (int i = 0; i < numbers.length; i++) {
                        if (i == numbers.length - 1) {
                            builder.append(numbers[i]);
                            break;
                        }
                        builder.append(numbers[i]).append(" ");
                    }
                    canvas.drawText(builder.toString(), 1010, 390, paint);
                    paint.setTextSize(70);
                    canvas.drawText("订单号：" + orderNumber, 1010, 540, paint);
                    mBluetoothUtil.printBitmap(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            private String handleString(String content) {
                try {
                    int spaceCount;
                    String value0 = startStation.getText().toString();
                    //一行装不下自动换行
                    if (value0.getBytes("GBK").length > 16)
                        value0 += "\n";
                    else {
                        spaceCount = 17 - value0.getBytes("GBK").length;
                        for (int i = 0; i < spaceCount; i++)
                            value0 += " ";
                    }
                    String value1 = endStation.getText().toString();
                    String value00 = startStationPhone.getText().toString();
                    spaceCount = 13 - value00.getBytes("GBK").length;
                    for (int i = 0; i < spaceCount; i++)
                        value00 += " ";
                    String value11 = endStationPhone.getText().toString();
                    String value2 = receiveAddress.getText().toString();
                    String value3 = sender.getText().toString();
                    if (value3.getBytes("GBK").length > 14)
                        value3 += "\n";
                    else {
                        spaceCount = 15 - value3.getBytes("GBK").length;
                        for (int i = 0; i < spaceCount; i++)
                            value3 += " ";
                    }
                    String value4 = num.getText().toString();
                    if (value4.getBytes("GBK").length > 8)
                        value4 += "\n";
                    else {
                        spaceCount = 9 - value4.getBytes("GBK").length;
                        for (int i = 0; i < spaceCount; i++)
                            value4 += " ";
                    }
                    String value5 = weight.getText().toString();
                    if (value5.getBytes("GBK").length > 4)
                        content = content.replace("重量", "\n重量");
                    String value6 = name.getText().toString();
                    if (value6.getBytes("GBK").length > 12)
                        value6 += "\n";
                    else {
                        spaceCount = 13 - value6.getBytes("GBK").length;
                        for (int i = 0; i < spaceCount; i++)
                            value6 += " ";
                    }
                    String spinner1 = spinner1Value;
                    String value7 = receiver.getText().toString();
                    if (value7.getBytes("GBK").length > 14)
                        value7 += "\n";
                    else {
                        spaceCount = 15 - value7.getBytes("GBK").length;
                        for (int i = 0; i < spaceCount; i++)
                            value7 += " ";
                    }
                    String value8 = phone.getText().toString();
                    String spinner2 = spinner2Value;
                    spaceCount = 17 - spinner2.getBytes("GBK").length;
                    for (int i = 0; i < spaceCount; i++)
                        spinner2 += " ";
                    String value9 = collection.getText().toString();
                    String value10 = startTime.getText().toString();
                    spaceCount = 19 - value10.getBytes("GBK").length;
                    for (int i = 0; i < spaceCount; i++)
                        value10 += " ";
                    return String.format(content, value0, value1, value00, value11, value2, value3, value4, value5, value6,
                            spinner1, value7, value8, spinner2, value9, value10);
                } catch (Exception e) {
                    e.printStackTrace();
                    return "";
                }
            }
        }).setView(contentView).create();
    }

    private boolean checkFill() {
        /*try {
            return !startStation.getText().toString().isEmpty() &&
                    !endStation.getText().toString().isEmpty() &&
                    !receiveAddress.getText().toString().isEmpty() &&
                    !sender.getText().toString().isEmpty() &&
                    !num.getText().toString().isEmpty() &&
                    !weight.getText().toString().isEmpty() &&
                    !name.getText().toString().isEmpty() &&
                    !receiver.getText().toString().isEmpty() &&
                    !phone.getText().toString().isEmpty() &&
                    !collection.getText().toString().isEmpty() &&
                    !startTime.getText().toString().isEmpty() &&
                    !endStationPhone.getText().toString().isEmpty() &&
                    !startStationPhone.getText().toString().isEmpty();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }*/
        return true;
    }

    private String spinner1Value, spinner2Value;

    private void initDialogViews(View contentView) {
        startStation = contentView.findViewById(R.id.start_station);
        startStationPhone = contentView.findViewById(R.id.start_station_phone);
        endStationPhone = contentView.findViewById(R.id.end_station_phone);
        endStation = contentView.findViewById(R.id.end_station);
        receiveAddress = contentView.findViewById(R.id.receive_address);
        sender = contentView.findViewById(R.id.sender);
        num = contentView.findViewById(R.id.num);
        weight = contentView.findViewById(R.id.weight);
        name = contentView.findViewById(R.id.name);
        receiver = contentView.findViewById(R.id.receiver);
        phone = contentView.findViewById(R.id.phone);
        collection = contentView.findViewById(R.id.collection);
        startTime = contentView.findViewById(R.id.start_time);
        Spinner dispatchMode = contentView.findViewById(R.id.dispatch_mode);
        Spinner payMode = contentView.findViewById(R.id.pay_mode);
        dispatchMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            String[] data = getResources().getStringArray(R.array.dispatch_mode);

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                spinner1Value = data[i];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                spinner1Value = data[0];
            }
        });
        payMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            String[] data = getResources().getStringArray(R.array.pay_mode);

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                spinner2Value = data[i];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                spinner2Value = data[0];
            }
        });
    }

    public void printOrder(View view) {
        if (checkIsConnected()) {
            if (mDialog == null)
                initDialog();
            mDialog.show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
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
        mBluetoothUtil.getBarCodeBitmap(content);
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
