package ca.igeneric.virusapp;

import android.Manifest;
import java.util.UUID;

interface Constants {

    int MESSAGE_STATE_CHANGE = 1;
    int MESSAGE_READ = 2;
    int MESSAGE_WRITE = 3;
    int MESSAGE_DEVICE_NAME = 4;
    int MESSAGE_TOAST = 5;

    String DEVICE_NAME = "device_name";
    String TOAST = "toast";
	
	String NAME_SECURE = "BluetoothChatSecure";
    String NAME_INSECURE = "BluetoothChatInsecure";

    UUID MY_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
    String[] permissions = new String[]{
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
}
