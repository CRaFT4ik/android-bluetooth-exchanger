package ru.er_log.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;


import java.util.UUID;

import ru.er_log.bluetooth.component.Utils;

public class MainActivity extends AppCompatActivity
{
    public static final String NAME = "eBluetoothService";
    public static final String EXTERNAL_DIR_NAME = Utils.getValidFileName(NAME);
    public static final String TAG = NAME;
    public static final UUID SERVICE_UUID = UUID.fromString("ac780fa2-0a80-43bc-af11-05715d5b5f09");

    public static final String REJECT_WORD = "_$reject!_";

    protected static final int ENABLE_BL_REQUEST_CODE = 0xDFA2;
    protected static final int DISCOVERY_BL_REQUEST_CODE = 0xDFA1;
    protected static final int DISCOVER_DURATION = 60;
    protected static final int CHOOSE_FILE_REQUEST_CODE = 0x7F26;

    private static final int INITIAL_REQUEST_CODE = 0x820A;
    private static final String[] INITIAL_PERMS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (!isPermissionsGranted())
                requestPermissions(INITIAL_PERMS, INITIAL_REQUEST_CODE);
        } // else
//        {
//            Toast.makeText(this, "Unsupported API version :(", Toast.LENGTH_LONG).show();
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
//                this.finishAffinity();
//            else
//                this.finish();
//        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if (requestCode == INITIAL_REQUEST_CODE)
        {
            if (!isPermissionsGranted())
            {
                Toast.makeText(this, "Can't continue without permissions", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Can't continue without permissions");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    this.finishAffinity();
                else
                    this.finish();
            }
        }
    }

    private boolean isPermissionsGranted()
    {
        boolean perm = true;

        perm &= hasPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        perm &= hasPermission(Manifest.permission.BLUETOOTH);
        perm &= hasPermission(Manifest.permission.BLUETOOTH_ADMIN);
        perm &= hasPermission(Manifest.permission.BLUETOOTH_ADMIN);
        perm &= hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            perm &= hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE);

        return perm;
    }

    private boolean hasPermission(String perm)
    {
        return (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, perm));
    }

    public void clickClientMode(View view)
    {
        Intent intent = new Intent(this, ClientActivity.class);
        startActivity(intent);
    }

    public void clickServerMode(View view)
    {
        Intent intent = new Intent(this, ServerActivity.class);
        startActivity(intent);
    }
}
