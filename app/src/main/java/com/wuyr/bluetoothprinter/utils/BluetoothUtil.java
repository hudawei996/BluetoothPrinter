package com.wuyr.bluetoothprinter.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Build;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.wuyr.bluetoothprinter.activities.MainActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by wuyr on 17-7-15 上午10:40.
 */

public class BluetoothUtil {

    @SuppressLint("StaticFieldLeak")
    private volatile static BluetoothUtil mInstance;
    public static final int DISCONNECT = 0, CONNECTING = 1, CONNECTED = 2;

    private static final UUID DEVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private Context mContext;
    private BluetoothAdapter mAdapter;
    private BroadcastReceiver mReceiver;
    private boolean isRegistered;
    private static int STATE;
    private ExecutorService mThreadPool;
    private List<Byte> mData;

    private BluetoothUtil(Context context) {
        mContext = context;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mThreadPool = Executors.newSingleThreadExecutor();
        mData = new ArrayList<>();
    }

    public static BluetoothUtil getInstance(Context context) {
        if (mInstance == null)
            synchronized (BluetoothUtil.class) {
                if (mInstance == null || mInstance.getContext() == null)
                    mInstance = new BluetoothUtil(context);
            }
        return mInstance;
    }

    public int getStatus() {
        return STATE;
    }

    public boolean isBluetoothOpen() {
        return mAdapter != null && mAdapter.isEnabled();
    }

    public boolean openBluetooth() {
        if (mAdapter != null) {
            if (mAdapter.isEnabled())
                return true;
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            intent.setAction(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            ((Activity) mContext).startActivityForResult(intent, MainActivity.OPEN_BLUETOOTH);
            return true;
        }
        return false;
    }

    private void closeBluetooth() {
        if (mAdapter != null && mAdapter.isEnabled()) {
            mAdapter.disable();
        }
    }

    private Context getContext() {
        return mContext;
    }

    public Set<BluetoothDevice> getBondedDevices() {
        if (STATE == CONNECTING)
            return new HashSet<>();
        checkBluetoothOpen();
        return mAdapter.getBondedDevices();
    }

    public void findDevices(OnFindDevicesListener listener) {
        if (STATE == CONNECTING) return;
        if (!mAdapter.isEnabled()) {
            //Toast.makeText(mContext, "please open bluetooth first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isRegistered)
            mContext.unregisterReceiver(mReceiver);
        registerReceiver(listener);
        mAdapter.startDiscovery();
    }

    public void setStatus(int state) {
        STATE = state;
    }

    public void stopFind() {
        if (mAdapter.isDiscovering())
            mAdapter.cancelDiscovery();
    }

    public synchronized void tryConnect(BluetoothDevice device, final OnConnectListener listener) {
        stopFind();
        if (STATE == DISCONNECT) {
            STATE = CONNECTING;
            new Thread() {
                @Override
                public void run() {
                    try {
                        BluetoothSocket socket = device.createRfcommSocketToServiceRecord(DEVICE_UUID);
                        socket.connect();
                        STATE = CONNECTED;
                        listener.successful(socket);
                    } catch (Exception e) {
                        listener.failure(e);
                    }
                }
            }.start();
        }
    }

    public boolean pairDevice(BluetoothDevice device) {
        if (STATE == CONNECTING) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) return device.createBond();
        else {
            try {
                Method createBond = device.getClass().getDeclaredMethod("createBond", BluetoothDevice.class);
                createBond.setAccessible(true);
                createBond.invoke(device);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    private void registerReceiver(final OnFindDevicesListener listener) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case BluetoothDevice.ACTION_FOUND:
                    case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                        try {
                            listener.deviceFounded(intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    default:
                        break;
                }
            }
        };
        mContext.registerReceiver(mReceiver, filter);
        isRegistered = true;
    }

    private void checkBluetoothOpen() {
        if (!mAdapter.isEnabled()) {
            openBluetooth();
        }
    }

    public void release() {
        closeBluetooth();
        if (isRegistered)
            mContext.unregisterReceiver(mReceiver);
        mContext = null;
        mAdapter = null;
        System.gc();
    }

    public interface OnFindDevicesListener {
        void deviceFounded(BluetoothDevice device);
    }

    public interface OnConnectListener {
        void successful(BluetoothSocket socket);

        void failure(Exception e);
    }

    public void printFullLine(String content, boolean isWidth2x) {
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
        try {
            addArrayToData(result.toString().getBytes("GBK"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void printFormatText(String content, PrintParams printParams) {
        try {
            if (printParams != null)
                setPrintParams(printParams);
            addArrayToData(content.getBytes("GBK"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void feed() {
        try {
            addArrayToData(" \n".getBytes("GBK"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void writeData(final BluetoothSocket socket) {
        if (socket == null) return;
        mThreadPool.execute(() -> {
            try {
                OutputStream os = socket.getOutputStream();
                os.write(getBytes());
                resetData();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void resetData() {
        mData = new ArrayList<>();
    }

    private void setPrintParams(PrintParams printParams) {
        byte temp = 0;
        if (printParams.getFont() == 1) {
            temp = 1;
        }

        if (printParams.getEmphasized() == 1) {
            temp = (byte) (temp | 8);
        }

        if (printParams.getHeight2x() == 1) {
            temp = (byte) (temp | 16);
        }

        if (printParams.getWidth2x() == 1) {
            temp = (byte) (temp | 32);
        }

        if (printParams.getUnderline() == 1) {
            temp = (byte) (temp | 128);
        }

        byte[] command = new byte[]{27, 33, temp};
        addArrayToData(printParams.getAlign());
        addArrayToData(command);
    }


    public void printBitmap(Bitmap bitmap) {
        addResizeBitmap(bitmap, 570, 0);
    }

    public Bitmap getBarCodeBitmap(String content) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(content,
                    BarcodeFormat.EAN_13, 1000, 280);
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    pixels[y * width + x] = matrix.get(x, y) ? 0xff000000 : 0xffffffff;
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(width, height,
                    Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void printBarCode(String content) {
       printBitmap(getBarCodeBitmap(content));
    }

    public void printQRCode(String content) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(content,
                    BarcodeFormat.QR_CODE, 1000, 1000);
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    pixels[y * width + x] = matrix.get(x, y) ? 0xff000000 : 0xffffffff;
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(width, height,
                    Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            printBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private byte[] getBytes() {
        byte[] bytes = new byte[mData.size()];
        for (int i = 0; i < mData.size(); i++) {
            int tmp = i >= mData.size() ? mData.size() - 1 : i;
            int tmp2 = i >= bytes.length ? bytes.length - 1 : i;
            bytes[tmp2] = mData.get(tmp);
        }
        return bytes;
    }

    private void addResizeBitmap(Bitmap bitmap, int nWidth, int nMode) throws NullPointerException {
        if (bitmap != null) {
            int width = (nWidth + 7) / 8 * 8;
            int height = bitmap.getHeight() * width / bitmap.getWidth();
            Bitmap grayBitmap = toGrayScale(bitmap);
            Bitmap rszBitmap = resizeImage(grayBitmap, width, height);
            int[] pixels = new int[rszBitmap.getWidth() * rszBitmap.getHeight()];
            byte[] src = new byte[rszBitmap.getWidth() * rszBitmap.getHeight()];
            Bitmap grayBitmap2 = toGrayScale(rszBitmap);
            grayBitmap2.getPixels(pixels, 0, rszBitmap.getWidth(), 0, 0, rszBitmap.getWidth(), rszBitmap.getHeight());
            int k = 0;
            for (int y = 0; y < grayBitmap2.getHeight(); ++y) {
                for (int x = 0; x < grayBitmap2.getWidth(); ++x) {
                    src[k] = (byte) ((pixels[k] & 255) > floyd16x16[x & 15][y & 15] ? 0 : 1);
                    ++k;
                }
            }
            byte[] command = new byte[8];
            height = src.length / width;
            command[0] = 29;
            command[1] = 118;
            command[2] = 48;
            command[3] = (byte) (nMode & 1);
            command[4] = (byte) (width / 8 % 256);
            command[5] = (byte) (width / 8 / 256);
            command[6] = (byte) (height % 256);
            command[7] = (byte) (height / 256);
            addArrayToData(command);
            byte[] codeContent = pixToEscRastBitImageCmd(src);

            for (byte tmp : codeContent) {
                mData.add(tmp);
            }
        } else {
            throw new NullPointerException("bitmap can't be null!");
        }
    }

    private void addArrayToData(byte[] array) {
        for (byte anArray : array)
            mData.add(anArray);
    }

    private byte[] pixToEscRastBitImageCmd(byte[] src) {
        byte[] data = new byte[src.length / 8];
        int i = 0;
        for (int k = 0; i < data.length; ++i) {
            data[i] = (byte) (p0[src[k]] + p1[src[k + 1]] + p2[src[k + 2]] + p3[src[k + 3]] + p4[src[k + 4]] + p5[src[k + 5]] + p6[src[k + 6]] + src[k + 7]);
            k += 8;
        }
        return data;
    }

    private Bitmap resizeImage(Bitmap bitmap, int w, int h) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = (float) w / (float) width;
        float scaleHeight = (float) h / (float) height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    private Bitmap toGrayScale(Bitmap bmpOriginal) {
        int height = bmpOriginal.getHeight();
        int width = bmpOriginal.getWidth();
        Bitmap bmpGrayScale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayScale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0.0F);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0.0F, 0.0F, paint);
        return bmpGrayScale;
    }

    private int[][] floyd16x16 = new int[][]{{
            0, 128, 32, 160, 8, 136, 40, 168, 2, 130, 34, 162, 10, 138, 42, 170},
            {192, 64, 224, 96, 200, 72, 232, 104, 194, 66, 226, 98, 202, 74, 234, 106},
            {48, 176, 16, 144, 56, 184, 24, 152, 50, 178, 18, 146, 58, 186, 26, 154},
            {240, 112, 208, 80, 248, 120, 216, 88, 242, 114, 210, 82, 250, 122, 218, 90},
            {12, 140, 44, 172, 4, 132, 36, 164, 14, 142, 46, 174, 6, 134, 38, 166},
            {204, 76, 236, 108, 196, 68, 228, 100, 206, 78, 238, 110, 198, 70, 230, 102},
            {60, 188, 28, 156, 52, 180, 20, 148, 62, 190, 30, 158, 54, 182, 22, 150},
            {252, 124, 220, 92, 244, 116, 212, 84, 254, 126, 222, 94, 246, 118, 214, 86},
            {3, 131, 35, 163, 11, 139, 43, 171, 1, 129, 33, 161, 9, 137, 41, 169},
            {195, 67, 227, 99, 203, 75, 235, 107, 193, 65, 225, 97, 201, 73, 233, 105},
            {51, 179, 19, 147, 59, 187, 27, 155, 49, 177, 17, 145, 57, 185, 25, 153},
            {243, 115, 211, 83, 251, 123, 219, 91, 241, 113, 209, 81, 249, 121, 217, 89},
            {15, 143, 47, 175, 7, 135, 39, 167, 13, 141, 45, 173, 5, 133, 37, 165},
            {207, 79, 239, 111, 199, 71, 231, 103, 205, 77, 237, 109, 197, 69, 229, 101},
            {63, 191, 31, 159, 55, 183, 23, 151, 61, 189, 29, 157, 53, 181, 21, 149},
            {254, 127, 223, 95, 247, 119, 215, 87, 253, 125, 221, 93, 245, 117, 213, 85}};
    private int[] p0 = new int[]{0, 128};
    private int[] p1 = new int[]{0, 64};
    private int[] p2 = new int[]{0, 32};
    private int[] p3 = new int[]{0, 16};
    private int[] p4 = new int[]{0, 8};
    private int[] p5 = new int[]{0, 4};
    private int[] p6 = new int[]{0, 2};

    // left = 0; center = 1; right = 2;
    public void setAlignNow(int position) {
        if (position < 0 || position > 2) return;
        addArrayToData(new byte[]{27, 97, (byte) position});
    }

    public static class PrintParams {
        private byte font, width2x, height2x, emphasized, underline;
        // left = 0; center = 1; right = 2;
        private byte[] align;

        public PrintParams() {
            this.align = new byte[]{27, 97, 0};
        }

        public byte[] getAlign() {
            return align;
        }

        public void setAlign(int position) {
            if (position < 0 || position > 2) return;
            align[2] = (byte) position;
        }

        public byte getFont() {
            return font;
        }

        public void setFont(int font) {
            this.font = (byte) font;
        }

        public byte getEmphasized() {
            return emphasized;
        }

        public void setEmphasized(int emphasized) {
            this.emphasized = (byte) emphasized;
        }

        public byte getWidth2x() {
            return width2x;
        }

        public void setWidth2x(int width2x) {
            this.width2x = (byte) width2x;
        }

        public byte getHeight2x() {
            return height2x;
        }

        public void setHeight2x(int height2x) {
            this.height2x = (byte) height2x;
        }

        public byte getUnderline() {
            return underline;
        }

        public void setUnderline(int underline) {
            this.underline = (byte) underline;
        }
    }
}
