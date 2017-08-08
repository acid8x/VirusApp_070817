package ca.igeneric.virusapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;

public class MainActivity extends Activity implements View.OnClickListener {

    private BluetoothAdapter mBluetoothAdapter = null;
    private BTService mBTService = null;
    private ImageView imageMain;
    private Button bt_status, scaleB;
    private TextView imageName;
    private Bitmap bmp = null;
    private int scale = 3;

    public static DisplayMetrics metrics;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        checkSelfPermissions();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            this.finish();
        }
    }

    @Override
    public void onClick(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.bt_status:
                if (mBTService.getState() != Constants.STATE_CONNECTED) {
                    intent = new Intent(MainActivity.this, DeviceListActivity.class);
                    startActivityForResult(intent, Constants.REQUEST_CONNECT_DEVICE_INSECURE);
                } else if (mBTService != null) {
                    mBTService.stop();
                }
                break;
            case R.id.open:
                intent = new Intent(MainActivity.this, FilesListActivity.class);
                startActivityForResult(intent, Constants.REQUEST_SELECT_FILE);
                break;
            case R.id.scale:
                scale++;
                if (scale == 4) scale = 1;
                scaleB.setText("SCALE x"+scale);
                break;
            case R.id.send:
                if (mBTService.getState() == Constants.STATE_CONNECTED) {
                    sendRGB565array();
                }
                else Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
                break;
            case R.id.sendM:
                if (mBTService.getState() == Constants.STATE_CONNECTED) {
                    int temp = scale;
                    scale = temp+2;
                    sendRGB565array();
                    scale = temp+1;
                    sendRGB565array();
                    scale = temp;
                    sendRGB565array();
                }
                else Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
                break;
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
            startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT);
        } else if (permissionsChecked) {
            if (mBTService == null) mBTService = new BTService(this, mHandler);
            else {
                if (mBTService.getState() == Constants.STATE_NONE) {
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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

    private void sendMessage(String message) {
        if (mBTService.getState() != Constants.STATE_CONNECTED) {
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
                    bt_status.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                    break;
                case Constants.MESSAGE_TOAST:
                    String error = "";
                    error += msg.getData().getString(Constants.TOAST);
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                    if (error.equals("Unable to connect device") || error.equals("Device connection was lost")) {
                        bt_status.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
                    }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Constants.REQUEST_CONNECT_DEVICE_SECURE:
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case Constants.REQUEST_CONNECT_DEVICE_INSECURE:
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case Constants.REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    mBTService = new BTService(this, mHandler);
                } else {
                    this.finish();
                }
            case Constants.REQUEST_SELECT_FILE:
                if (resultCode == Activity.RESULT_OK) {
                    String fileName = "";
                    fileName += data.getStringExtra("FILE");
                    bmp = BitmapFactory.decodeFile(fileName);
                    imageMain.setImageBitmap(getResizedBitmap(bmp,MainActivity.metrics.widthPixels,MainActivity.metrics.heightPixels));
                    String name = "";
                    for (char c : fileName.toCharArray()) {
                        if (c == '/') name = "";
                        else name += c;
                    }
                    imageName.setText(name);
                } else onDestroy();
                break;
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mBTService.connect(device, secure);
    }

    private void sendRGB565array() {
        if (bmp != null) {
            Bitmap bmp2 = getResizedBitmap(bmp,320/scale,480/scale);
            char d = 44, s = 36, e = 38;
            int x = 0;
            int y = 200;
            int w = bmp2.getWidth();
            int h = bmp2.getHeight();
            String msg = ""+s+x+d+y+d+w+d+h+d+scale+e;
            sendMessage(msg);
            mBTService.write(rgb565ValuesFromBitmap(bmp2));
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

    public static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
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

    private void initViews() {
        bt_status = findViewById(R.id.bt_status);
        bt_status.setOnClickListener(this);
        Button open = findViewById(R.id.open);
        open.setOnClickListener(this);
        scaleB = findViewById(R.id.scale);
        scaleB.setOnClickListener(this);
        Button send = findViewById(R.id.send);
        send.setOnClickListener(this);
        Button sendM = findViewById(R.id.sendM);
        sendM.setOnClickListener(this);
        imageMain = findViewById(R.id.imageMain);
        imageName = findViewById(R.id.imageName);
    }

    private boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            onDestroy();
            return;
        }
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }
}
