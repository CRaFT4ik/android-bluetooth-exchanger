package ru.er_log.bluetooth.util;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.format.DateFormat;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Date;

public class ScreenshotUtil
{
    public static File saveScreenshot(File dir, byte[] bytes)
    {
        String pngName = DateFormat.format("dd.MM.yyyy_hh_mm_ss", new Date()) + ".png";
        return FileUtil.saveFile(dir, pngName, bytes);
    }

    public static ByteArrayOutputStream takeScreenshot(Activity activity)
    {
        View v1 = activity.getWindow().getDecorView().getRootView();
        v1.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
        v1.setDrawingCacheEnabled(false);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);

        return stream;
    }

    public static void openScreenshot(Activity activity, File imageFile)
    {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(imageFile);
        intent.setDataAndType(uri, "image/*");
        activity.startActivity(intent);
    }
}
