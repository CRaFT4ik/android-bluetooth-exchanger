package ru.er_log.bluetooth.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Random;

import static ru.er_log.bluetooth.MainActivity.TAG;

public class FileUtil
{
    private static final int EOF = -1;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    public static File uriToFile(Context context, Uri uri) throws IOException
    {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);

        String fileName = getFileName(context, uri);
        String[] splitName = splitFileName(fileName);
        File tempFile = File.createTempFile(splitName[0], splitName[1]);
        tempFile = rename(tempFile, fileName);
        tempFile.deleteOnExit();

        FileOutputStream out = null;
        try { out = new FileOutputStream(tempFile); }
        catch (FileNotFoundException e) { e.printStackTrace(); }

        if (inputStream != null)
        {
            copy(inputStream, out);
            inputStream.close();
        }

        if (out != null)
            out.close();

        return tempFile;
    }

    public static String[] splitFileName(String fileName)
    {
        String name = fileName;
        String extension = "";
        int i = fileName.lastIndexOf(".");
        if (i != -1)
        {
            name = fileName.substring(0, i);
            extension = fileName.substring(i);
        }

        Random random = new Random();
        while (name.length() < 3)
            name += random.nextInt(10);

        return new String[] {name, extension};
    }

    public static String getFileName(Context context, Uri uri)
    {
        String result = null;
        if (uri.getScheme().equals("content"))
        {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try
            {
                if (cursor != null && cursor.moveToFirst())
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            } catch (Exception e)
            {
                e.printStackTrace();
            } finally
            {
                if (cursor != null)
                    cursor.close();
            }
        }
        if (result == null)
        {
            result = uri.getPath();
            int cut = result.lastIndexOf(File.separator);
            if (cut != -1)
                result = result.substring(cut + 1);
        }
        return result;
    }

    public static File rename(File file, String newName)
    {
        File newFile = new File(file.getParent(), newName);
        if (!newFile.equals(file))
        {
            if (newFile.exists() && newFile.delete())
                Log.d(TAG, "Delete old " + newName + " file");

            if (file.renameTo(newFile))
                Log.d(TAG, "Rename file to " + newName);
        }
        return newFile;
    }

    public static long copy(InputStream input, OutputStream output) throws IOException
    {
        long count = 0;
        int n;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        while (EOF != (n = input.read(buffer)))
        {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public static byte[] readFile(File file)
    {
        int size = (int) file.length();
        byte[] bytes = new byte[size];

        try
        {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        return bytes;
    }

    public static File saveFile(File dir, String fileName, byte[] bytes)
    {
        File file = new File(dir, fileName);

        file.getParentFile().mkdirs();
        try { file.createNewFile(); }
        catch (IOException e) {}

        try
        {
            FileOutputStream fileStream = new FileOutputStream(file);
            fileStream.write(bytes);
        } catch (IOException e)
        {
            Log.e(TAG, "Error saving file '" + file + "' on disk", e);
            return null;
        }

        String path;
        try { path = file.getCanonicalPath(); }
        catch (IOException e) { path = file.getAbsolutePath(); }

        Log.d(TAG, "File saved: " + path);
        return file;
    }

    public static boolean isFilenameValid(String file)
    {
        try
        {
            new File(file).getCanonicalPath();
            return true;
        } catch (IOException e)
        {
            return false;
        }
    }

    public static final class FileCollector
    {
        private String fileName;
        private File file;
        private byte[] data;

        public FileCollector() { }

        public void set(String fileName, File file, byte[] data)
        {
            this.fileName = fileName;
            this.file = file;
            this.data = data;
        }

        public File getFile()
        {
            return file;
        }

        public String getName()
        {
            return fileName;
        }

        public byte[] getData()
        {
            return data;
        }

        public void reset()
        {
            fileName = null;
            file = null;
            data = null;
        }
    }
}
