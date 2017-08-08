package ca.igeneric.virusapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.widget.Toast;

import java.nio.ByteBuffer;

public class MainActivity extends Activity {

    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final int REQUEST_SELECT_FILE = 4;

    private BluetoothAdapter mBluetoothAdapter = null;
    private BTService mBTService = null;

    int scale = 3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkSelfPermissions();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            this.finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBTService != null) {
            mBTService.stop();
        }
        finishAndRemoveTask();
        System.exit(0);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else if (permissionsChecked) {
            if (mBTService == null) setupChat();
            else {
                if (mBTService.getState() == BTService.STATE_NONE) {
                    mBTService.start();
                }
            }
        }
    }

    int permissionsGranted = 0;
    boolean permissionsChecked = false;

    private void checkSelfPermissions() {
        if (Build.VERSION.SDK_INT >= 23) requestPermissions(Constants.permissions, 0);
        else permissionsChecked = true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0) {
            switch (requestCode) {
                case 0:
                    for (int results : grantResults) {
                        if (results != PackageManager.PERMISSION_GRANTED) onDestroy();
                        else permissionsGranted++;
                        if (permissionsGranted == Constants.permissions.length) permissionsChecked = true;
                    }
                    break;
            }
        }
    }

    private void setupChat() {
        mBTService = new BTService(this, mHandler);
        Intent listIntent = new Intent(MainActivity.this, DeviceListActivity.class);
        startActivityForResult(listIntent, REQUEST_CONNECT_DEVICE_INSECURE);
    }

    private void sendMessage(String message) {
        if (mBTService.getState() != BTService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.length() > 0) {
            message = "$" + message + "&";
            byte[] send = message.getBytes();
            mBTService.write(send);
        }
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    break;
                case Constants.MESSAGE_WRITE:
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Toast.makeText(MainActivity.this, readMessage, Toast.LENGTH_LONG).show();
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    String mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Toast.makeText(MainActivity.this, "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    Intent listIntent = new Intent(MainActivity.this, FilesListActivity.class);
                    startActivityForResult(listIntent, REQUEST_SELECT_FILE);
                    break;
                case Constants.MESSAGE_TOAST:
                    String error = "";
                    error += msg.getData().getString(Constants.TOAST);
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                    if (error.equals("Unable to connect device") || error.equals("Device connection was lost")) {
                        Intent serverIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                    }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    setupChat();
                } else {
                    this.finish();
                }
            case REQUEST_SELECT_FILE:
                if (resultCode == Activity.RESULT_OK) {
                    String fileName = "";
                    fileName += data.getStringExtra("FILE");
                    Toast.makeText(MainActivity.this, fileName, Toast.LENGTH_LONG).show();
                    sendRGB565array(fileName);
                } else onDestroy();
                break;
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mBTService.connect(device, secure);
    }

    private void sendRGB565array(String file) {
        Bitmap bmp = BitmapFactory.decodeFile(file);
        if (bmp != null) {
            bmp = getResizedBitmap(bmp,320/scale,480/scale);
            char d = 44, s = 36, e = 38;
            int x = 0;
            int y = 200;
            int w = bmp.getWidth();
            int h = bmp.getHeight();
            String msg = ""+s+x+d+y+d+w+d+h+d+scale+e;
            sendMessage(msg);
            byte[] toSend = rgb565ValuesFromBitmap(bmp);
            mBTService.write(toSend);
        }
    }

    private byte[] rgb565ValuesFromBitmap(Bitmap orig) {
        Bitmap maskBitmap = Bitmap.createBitmap( orig.getWidth(), orig.getHeight(), Bitmap.Config.RGB_565 );
        Canvas c = new Canvas();
        c.setBitmap(maskBitmap);
        Paint p = new Paint();
        p.setFilterBitmap(true);
        c.drawBitmap(orig,0,0,p);
        orig.recycle();
        int bytes = maskBitmap.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes);
        maskBitmap.copyPixelsToBuffer(buffer);
        return buffer.array();
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        float scaleRatio = scaleWidth;
        if (scaleHeight < scaleWidth) scaleRatio = scaleHeight;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleRatio, scaleRatio);
        return Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
    }
}
