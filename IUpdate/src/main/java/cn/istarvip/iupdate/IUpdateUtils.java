package cn.istarvip.iupdate;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * site:istarvip.cn
 * 作者：Administrator on 2017/3/15 11:53
 * 邮箱：917017530@qq.com
 * FIXME 更新工具类
 */

public class IUpdateUtils {
    protected IUpdateUtils() {}

    public static void closeQuietly(Closeable c) {
        if (c == null) return;
        try {c.close();} catch (Throwable ignored) {}
    }

    public static Date dayBegin(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static String formatToMegaBytes(long bytes) {
        double megaBytes = bytes / 1048576.0;
        if (megaBytes < 1) return new DecimalFormat("0.0").format(megaBytes);
        return new DecimalFormat("#.0").format(megaBytes);
    }

    public static Subscription getMd5ByFile(final File file, Subscriber<String> md5Subscriber) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(file);
                    MappedByteBuffer byteBuffer = fis.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
                    MessageDigest md5 = MessageDigest.getInstance("MD5");
                    md5.update(byteBuffer);
                    BigInteger bi = new BigInteger(1, md5.digest());
                    subscriber.onNext(String.format("%032x", bi));
                } catch (Throwable t) {
                    subscriber.onError(t);
                } finally {
                    closeQuietly(fis);
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(md5Subscriber);
    }

    public static String getMd5ByFile(File file) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            MappedByteBuffer byteBuffer = fis.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(byteBuffer);
            BigInteger bi = new BigInteger(1, md5.digest());
            return String.format("%032x", bi);
        } catch (Exception e) {
            e.printStackTrace();
            return file.toString();
        } finally {
            closeQuietly(fis);
        }
    }

    public static String getApplicationName(Context app) {
        PackageManager pm;
        ApplicationInfo ai;
        try {
            pm = app.getPackageManager();
            ai = pm.getApplicationInfo(app.getPackageName(), 0);
            return pm.getApplicationLabel(ai).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return app.getPackageName();
        }
    }

    public static int getVersionCode(Context app) {
        PackageManager pm = app.getPackageManager();
        PackageInfo pi;
        try {
            pi = pm.getPackageInfo(app.getPackageName(), 0);
            return pi.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
    }

    public static String getVersionName(Context app) {
        PackageManager pm = app.getPackageManager();
        PackageInfo pi;
        try {
            pi = pm.getPackageInfo(app.getPackageName(), 0);
            return pi.versionName;
        } catch (Exception e) {
            e.printStackTrace();
            return app.getPackageName();
        }
    }

    public static int getAppIconResId(Context app) {
        PackageManager pm = app.getPackageManager();
        String packageName = app.getPackageName();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return ai.icon;
        } catch (Exception e) {
            e.printStackTrace();
            try {
                return app.getResources().getIdentifier("sym_def_app_icon", "mipmap", "android");
            } catch (Exception e1) {
                e1.printStackTrace();
                return 0;
            }
        }
    }
    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            if (info.getState() == NetworkInfo.State.CONNECTED) {
                return true;
            }
        }
        return false;
    }
    public static boolean isWifi(Context context) {
        NetworkInfo networkInfo = ((ConnectivityManager) (context.getSystemService(Context.CONNECTIVITY_SERVICE))).getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected() && (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET || networkInfo.getType() == 17 || networkInfo.getType() == -1 || networkInfo.getType() == 13 || networkInfo.getType() == 16);
    }
}
