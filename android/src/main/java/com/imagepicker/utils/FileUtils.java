package com.imagepicker.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import java.io.File;

public class FileUtils {

    public static String createRootPath(Context context) {
        String cacheRootPath = "";
        if (Build.VERSION.SDK_INT <= 28) {
            if (isSdCardAvailable()) {
                cacheRootPath = context.getExternalCacheDir().getPath();
            } else {
                cacheRootPath = context.getCacheDir().getPath();
            }
        } else {
            File[] medias = context.getExternalMediaDirs();
            if (medias != null && medias.length > 0) {
                cacheRootPath = medias[0].getPath();
            } else {
                cacheRootPath = Environment.getExternalStorageDirectory().getPath() + "/Android/media/temp";
            }
        }
        createDir(cacheRootPath);
        return cacheRootPath;
    }

    public static boolean isSdCardAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static String createDir(String dirPath) {
        try {
            File file = new File(dirPath);
            if (file.getParentFile() != null && file.getParentFile().exists()) {
                file.mkdir();
                return file.getAbsolutePath();
            } else {
                createDir(file.getParentFile().getAbsolutePath());
                file.mkdir();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dirPath;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static String createFile(File file) {
        try {
            if (file.getParentFile() != null && file.getParentFile().exists()) {
                file.createNewFile();
                return file.getAbsolutePath();
            } else {
                createDir(file.getParentFile().getAbsolutePath());
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getApplicationId(Context appContext) throws IllegalArgumentException {
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo = appContext.getPackageManager().getApplicationInfo(appContext.getPackageName(), PackageManager.GET_META_DATA);
            if (applicationInfo == null) {
                throw new IllegalArgumentException(" get application info = null, has no meta data! ");
            }
            return applicationInfo.metaData.getString("APP_ID");
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalArgumentException(" get application info error! ", e);
        }
    }
}
