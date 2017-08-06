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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.wuyr.bluetoothprinter2.R;
import com.wuyr.bluetoothprinter2.customize.MySnackBar;
import com.wuyr.bluetoothprinter2.utils.BluetoothUtil;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static android.os.Build.VERSION_CODES.M;
import static com.wuyr.bluetoothprinter2.R.array.count;

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
            mBluetoothUtil.printFormatText(new SimpleDateFormat("下单时间：yyyy-MM-dd HH:mm:ss\n",
                    Locale.getDefault()).format(new Date()), printParams);
            mBluetoothUtil.printFullLine("-", isWidth2x(printParams));
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_bill_view, null);
            EditText remarks = dialogView.findViewById(R.id.remarks);
            Spinner food1 = dialogView.findViewById(R.id.food1);
            Spinner food2 = dialogView.findViewById(R.id.food2);
            Spinner food3 = dialogView.findViewById(R.id.food3);
            Spinner food4 = dialogView.findViewById(R.id.food4);
            Spinner food5 = dialogView.findViewById(R.id.food5);
            Spinner food6 = dialogView.findViewById(R.id.food6);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getResources().getStringArray(count));
            food1.setAdapter(adapter);
            food2.setAdapter(adapter);
            food3.setAdapter(adapter);
            food4.setAdapter(adapter);
            food5.setAdapter(adapter);
            food6.setAdapter(adapter);
            initData();
            food1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    putData("小炒羊肉", Integer.parseInt(adapter.getItem(i)));
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
            food2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    putData("蚝油西兰花", Integer.parseInt(adapter.getItem(i)));
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
            food3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    putData("胡萝卜炒虾仁", Integer.parseInt(adapter.getItem(i)));
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
            food4.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    putData("香醋角瓜", Integer.parseInt(adapter.getItem(i)));
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
            food5.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    putData("青椒肉丝炒年糕", Integer.parseInt(adapter.getItem(i)));
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
            food6.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    putData("清熘鸳鸯豆腐", Integer.parseInt(adapter.getItem(i)));
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
            EditText editText = dialogView.findViewById(R.id.discount);
            new AlertDialog.Builder(this).setView(dialogView).setCancelable(false).setPositiveButton("确定", (dialogInterface, i) -> {
                int discount = TextUtils.isEmpty(editText.getText().toString()) ? 0 : Integer.parseInt(editText.getText().toString());
                Map<String, Integer> price = getFoodPrice();
                Set<String> keys = price.keySet();
                StringBuilder builder = new StringBuilder();
                builder.append(handleString("美食篮子/&/数量/&/单价/&/金额\n", isWidth2x(printParams)));
                builder.append(getFullLine("-", isWidth2x(printParams)));
                int count = 0;
                for (String k : keys) {
                    if (mData.get(k) > 0) {
                        count += mData.get(k) * price.get(k);
                        builder.append(handleString(String.format(Locale.getDefault(),
                                "%s/&/%d/&/%d/&/%d\n", k, mData.get(k), price.get(k),
                                mData.get(k) * price.get(k)), isWidth2x(printParams)));
                    }
                }
                builder.append(getFullLine("-", isWidth2x(printParams)));
                builder.append(handleString(String.format(Locale.getDefault(), "小计/&/1/&/--/&/%d\n", count), isWidth2x(printParams)));
                builder.append(handleString(String.format(Locale.getDefault(), "优惠折扣/&/1/&/-%d/&/-%d\n", discount, discount), isWidth2x(printParams)));
                builder.append(getFullLine("-", isWidth2x(printParams)));
                mBluetoothUtil.printFormatText(builder.toString(), printParams);
                builder.append(getFullLine("-", isWidth2x(printParams)));
                printParams.setHeight2x(1);
                printParams.setWidth2x(1);
                mBluetoothUtil.printFormatText(String.format(Locale.getDefault(), "合计：%s (已付款)\n", count - discount), printParams);
                printParams.setHeight2x(0);
                printParams.setWidth2x(0);
                mBluetoothUtil.printFormatText("", printParams);
                mBluetoothUtil.printFullLine("-", isWidth2x(printParams));
                printParams.setHeight2x(1);
                printParams.setWidth2x(1);
                mBluetoothUtil.printFormatText(String.format(Locale.getDefault(), "备注：\n      %s\n", remarks.getText().toString()), printParams);
                mBluetoothUtil.feed();
                mBluetoothUtil.feed();
                mBluetoothUtil.feed();
                mBluetoothUtil.writeData(mSocket);
            }).setNegativeButton("取消", null).show();
        }
    }

    private String getFullLine(String content, boolean isWidth2x) {
        int count = isWidth2x ? 24 : 48;
        char[] items = content.toCharArray();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) {
            for (char tmp : items) {
                if (result.length() == count) break;
                result.append(tmp);
            }
            if (result.length() == count) break;
        }
        result.append("\n");
        return result.toString();
    }

    Map<String, Integer> mData = new HashMap<>();

    private void initData() {
        Set<String> keys = getFoodPrice().keySet();
        for (String k : keys)
            mData.put(k, 0);
    }

    private void putData(String k, int v) {
        mData.put(k, v);
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
        String[] items = src.split("/&/");
        int totalLength = isWidth2x ? 24 : 48;
        StringBuilder result = new StringBuilder();
        int stringLength = 0;
        int stringCount = 0;
        for (String tmp : items) {
            if (!tmp.isEmpty()) {
                stringLength += getStringRealLength(tmp);
                stringCount++;
            }
        }
        if (stringLength > totalLength) {
            int moreLine = calculateLine(totalLength, stringLength);
            totalLength += (moreLine * totalLength);
        }
        if (stringCount - 1 < 1)
            return src;
        int spaceLength = Math.abs((totalLength - stringLength) / (stringCount - 1));
        for (int i = 0; i < stringCount; i++) {
            result.append(items[i]);
            if (i == stringCount - 1) break;
            for (int count = 0; count < spaceLength; count++) {
                result.append(" ");
            }
        }
        return result.toString();
    }

    private Map<String, Integer> getFoodPrice() {
        Map<String, Integer> price = new HashMap<>();
        price.put("小炒羊肉", 12);
        price.put("蚝油西兰花", 13);
        price.put("胡萝卜炒虾仁", 14);
        price.put("青椒肉丝炒年糕", 15);
        price.put("香醋角瓜", 16);
        price.put("清熘鸳鸯豆腐", 17);
        price.put("腐烧杏鲍菇", 18);
        price.put("油面筋塞肉", 19);
        price.put("腐乳烧土豆", 20);
        price.put("娃娃菜炒粉丝", 22);
        return price;
    }

    private int calculateLine(int total, int target) {
        int line = 0;
        do {
            ++line;
        } while ((target -= total) >= total);
        return line;
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
        int stringLength = getStringRealLength(src);
        int spaceCount = (totalLength / 2) - (stringLength / 2);
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
            /*try {
                Bitmap bitmap = getBitmapFromAssets("p3.jpg");
                if (bitmap != null)
                    mBluetoothUtil.printBitmap(bitmap);
                mBluetoothUtil.feed();
                mBluetoothUtil.feed();
                mBluetoothUtil.writeData(mSocket);
            } catch (Exception e) {
                e.printStackTrace();
            }*/
            BluetoothUtil.PrintParams printParams = new BluetoothUtil.PrintParams();
            printParams.setAlign(1);
            printParams.setEmphasized(1);
            printParams.setWidth2x(1);
            printParams.setHeight2x(1);
            mBluetoothUtil.printFormatText("好客速运\n\n", printParams);

            printParams.setAlign(0);
            printParams.setEmphasized(0);
            printParams.setWidth2x(0);
            printParams.setHeight2x(0);

            String content = "订单号: 123456789012\n" +
                    "From: XXXXXXXXXXX站\n" +
                    "To: XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX站\n" +
                    "收件地址: XX市XX区XXXXXXXXXXXXXXXX\n\n" +

                    "托 运 人: 一二三四        联系方式: 0758-1234755\n" +
                    "货物名称: 一二三四        件    数: 123  \n" +
                    "配送方式: 系统范围价配送\n\n" +


                    "收 件 人: 一二三四        联系方式: 0758-1234756\n" +
                    "费用合计: 9999            付款方式: 寄方/支付宝\n\n";
            /*String content = "From: XXXXXXXXXXX站\n" +
                    "To: XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX站\n" +
                    "收件地址: XX市XX区XXXXXXXXXXXXXXXX\n\n" +

                    "托 运 人: 一二三四        件    数: 12345\n" +
                    "货物名称: 一二三四        配送方式: 送货上门\n" +
                    "收 件 人: 一二三四        联系电话: 0758-1234756\n\n" +

                    "费用合计: 9999            实收费用: 8888\n" +
                    "付款方式: 寄方/支付宝支付 代收货款: 9999\n\n" +

                    "计划发车时间: 2017-08-05 15:00\n" +
                    "          (以实际发车为准,短信通知)\n\n" +
                    "目的站电话: 0057-4123456\n" +
                    "始发站电话: 0758-7456456\n\n";*/
            mBluetoothUtil.printFormatText(handleString(content, false), printParams);
            String content2 = "备注:\n" +
                    "┏━━━━━━━━━━┓\n" +
                    "┃快递大哥请你轻拿轻放┃\n" +
                    "┃快递大哥请你轻拿轻放┃\n" +
                    "┃尽快送达谢谢        ┃\n" +
                    "┗━━━━━━━━━━┛\n";
            printParams.setEmphasized(1);
            printParams.setWidth2x(1);
            printParams.setHeight2x(1);
            //mBluetoothUtil.printFormatText(content2, printParams);

            mBluetoothUtil.setAlignNow(1);
            //mBluetoothUtil.printCode("387534535881");

            printParams.setAlign(1);
            printParams.setEmphasized(1);
            printParams.setWidth2x(1);
            printParams.setHeight2x(1);
            //mBluetoothUtil.printFormatText("45777376423", printParams);

            mBluetoothUtil.feed();
            mBluetoothUtil.feed();
            mBluetoothUtil.feed();
            mBluetoothUtil.writeData(mSocket);
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
