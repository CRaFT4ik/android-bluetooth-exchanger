package ru.er_log.bluetooth.component;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

import ru.er_log.bluetooth.MainActivity;

import static ru.er_log.bluetooth.MainActivity.TAG;

public class Utils
{
    public static void enableBluetooth(Activity activity, BluetoothAdapter bluetoothAdapter, int requestCode)
    {
        if (bluetoothAdapter == null)
        {
            Toast.makeText(activity.getBaseContext(), "Bluetooth is not supported on this device", Toast.LENGTH_LONG).show();
            return;
        }

//        if (!bluetoothAdapter.isEnabled())
//        {
            // Just enable bluetooth.
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, requestCode);
//        }
    }

    public static void enableDiscoverability(Activity activity, BluetoothAdapter bluetoothAdapter, int duration, int requestCode)
    {
        if (bluetoothAdapter == null)
        {
            Toast.makeText(activity.getBaseContext(), "Bluetooth is not supported on this device", Toast.LENGTH_LONG).show();
            return;
        }

//        if (!bluetoothAdapter.isEnabled())
//        {
            // Enable bluetooth & discovery.
            Intent discoveryIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoveryIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration);
            activity.startActivityForResult(discoveryIntent, requestCode);
//        }
    }

    public static String getValidFileName(String fileName)
    {
        String newFileName = fileName.replaceAll("^[.\\\\/:*?\"<>|]?[\\\\/:*?\"<>|]*", "");
        if (newFileName.length() == 0)
            throw new IllegalStateException("File Name " + fileName + " results in a empty fileName!");
        return newFileName;
    }

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable()
    {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state))
            return true;

        return false;
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable()
    {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))
            return true;

        return false;
    }

    public static File getPrivateScreenshotsDir(Context context)
    {
        File file = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        file.mkdirs();

        return file;
    }

    public static File getPrivateDocumentsDir(Context context)
    {
        File file = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        file.mkdirs();

        return file;
    }
}
