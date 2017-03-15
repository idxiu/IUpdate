package cn.istarvip.iupdate;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.text.TextUtils;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import cn.istarvip.iupdate.event.EventBusUtils;
import cn.istarvip.iupdate.event.UpdateEvent;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * site:istarvip.cn
 * 作者：Administrator on 2017/3/15 11:51
 * 邮箱：917017530@qq.com
 * FIXME
 */

public class IUpdateApi {
    protected IUpdateApi() {
    }

    protected static IUpdateApi sInstance;

    protected boolean mForceUpdate;
    protected IUpdateBean mUpdateBeanProvided;
    protected String mJsonUrl;
    protected int mIconResId;
    protected boolean mShowDialogIfWifi;

    public void forceUpdate(Activity activity) {
        mForceUpdate = true;
        update(activity);
    }

    public void update(final Activity activity) {
        if (mUpdateBeanProvided == null && TextUtils.isEmpty(mJsonUrl)) {
            System.err.println("Please set updateBean or mJsonUrl.");
            mForceUpdate = false;
            return;
        }
        if (mUpdateBeanProvided != null) {
            updateMatters(mUpdateBeanProvided, activity);
        } else {
            Observable.create(new Observable.OnSubscribe<String>() {
                @Override
                public void call(Subscriber<? super String> subscriber) {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder().url(mJsonUrl).build();
                    Response response;
                    try {
                        response = client.newCall(request).execute();
                        if (response.isSuccessful()) {
                            subscriber.onNext(response.body().string());
                        } else {
                            subscriber.onError(new IOException(response.code() + ": " + response.body().string()));
                        }
                    } catch (Throwable t) {
                        subscriber.onError(t);
                    }
                }
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<String>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                    mForceUpdate = false;
                    EventBusUtils.post(new UpdateEvent(2, IConstants.requestError));
                }

                @Override
                public void onNext(String responseBody) {
                    if (IConstants.debugMode) System.out.println(responseBody);
                    Gson gson = new Gson();
                    try {
                        IUpdateBean iUpdateBean = gson.fromJson(responseBody, cn.istarvip.iupdate.IUpdateBean.class);
                        if (iUpdateBean!=null)iUpdateBean.toString();
                        updateMatters(iUpdateBean, activity);
                    } catch (Exception e) {
                        e.printStackTrace();
                        mForceUpdate = false;
                        EventBusUtils.post(new UpdateEvent(2, IConstants.jsonError));
                        return;
                    }
                }
            });
        }
    }

    protected void updateMatters(final IUpdateBean updateBean, final Activity activity) {
        final int currentCode = IUpdateUtils.getVersionCode(activity.getApplicationContext());
        final int versionCode = updateBean.versionCode;
        final String versionName = updateBean.versionName;
        if (currentCode < versionCode) {
            EventBusUtils.post(new UpdateEvent(1, true, updateBean));
            final SharedPreferences sp = activity.getSharedPreferences("update", Context.MODE_PRIVATE);
            long lastIgnoredDayBegin = sp.getLong("time", 0);
            int lastIgnoredCode = sp.getInt("versionCode", 0);
            long todayBegin = IUpdateUtils.dayBegin(new Date()).getTime();
            if (!mForceUpdate && todayBegin == lastIgnoredDayBegin && versionCode == lastIgnoredCode) {
                mForceUpdate = false;
                return;
            }
            final File file = new File(activity.getExternalCacheDir(), "update.apk");
            if (file.exists()) {
                IUpdateUtils.getMd5ByFile(file,   new Subscriber<String>() {

                    boolean fileExists = false;

                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        file.delete();
                        e.printStackTrace();
                        proceedToUI(sp, file, fileExists, activity, versionName, updateBean, versionCode);
                    }

                    @Override
                    public void onNext(String md5JustDownloaded) {
                        String md5InUpdateBean = updateBean.md5;
                        if (md5JustDownloaded.equalsIgnoreCase(md5InUpdateBean)) {
                            fileExists = true;
                        } else {
                            file.delete();
                            EventBusUtils.post(new UpdateEvent(2, IConstants.md5Error));
                            System.err.println("MD5 mismatch. md5JustDownloaded: " + md5JustDownloaded + ". md5InUpdateBean: " + md5InUpdateBean + ".");
                        }
                        proceedToUI(sp, file, fileExists, activity, versionName, updateBean, versionCode);
                    }
                });
            } else {
                proceedToUI(sp, file, false, activity, versionName, updateBean, versionCode);
            }
        } else {
            EventBusUtils.post(new UpdateEvent(1, false, updateBean));
        }
        mForceUpdate = false;
    }

    protected void proceedToUI(SharedPreferences sp, File file, boolean fileExists, Activity activity, String versionName, IUpdateBean IUpdateBean, int versionCode) {
        if (mForceUpdate || (mShowDialogIfWifi && IUpdateUtils.isWifi(activity.getApplicationContext()))) {
            showAlertDialog(sp, file, fileExists, activity, versionName, IUpdateBean, versionCode);
        } else {
            showNotification(sp, file, fileExists, activity, versionName, IUpdateBean, versionCode);
        }
    }

    @SuppressWarnings("ResourceType")
    protected void showNotification(final SharedPreferences sp, final File file, final boolean fileExists, final Activity activity, final String versionName, final IUpdateBean IUpdateBean, final int versionCode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) return;
        activity.getApplicationContext().registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                showAlertDialog(sp, file, fileExists, activity, versionName, IUpdateBean, versionCode);
            }
        }, new IntentFilter("cn.istarvip.iupdate.dialog"));
        activity.getApplicationContext().registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                sp.edit()
                        .putLong("time", IUpdateUtils.dayBegin(new Date()).getTime())
                        .putInt("versionCode", versionCode)
                        .putString("versionName", versionName)
                        .apply();
            }
        }, new IntentFilter("cn.istarvip.iupdate.Ignore"));
        int smallIconResId = mIconResId > 0 ? mIconResId : IUpdateUtils.getAppIconResId(activity.getApplicationContext());
        String title = IUpdateUtils.getApplicationName(activity.getApplicationContext()) + " " + versionName + " " + IConstants.hintText;
        Notification.Builder builder = new Notification.Builder(activity)
                .setAutoCancel(true)
                .setTicker(title)
                .setSmallIcon(smallIconResId)
                .setContentTitle(title)
                .setContentText(IUpdateBean.msg)
                .setContentIntent(PendingIntent.getBroadcast(activity.getApplicationContext(), 1, new Intent("cn.istarvip.iupdate.dialog"), PendingIntent.FLAG_CANCEL_CURRENT))
                .setDeleteIntent(PendingIntent.getBroadcast(activity.getApplicationContext(), 2, new Intent("cn.istarvip.iupdate.Ignore"), PendingIntent.FLAG_CANCEL_CURRENT));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setShowWhen(true);
            builder.setVibrate(new long[0]);
        }
        builder.setPriority(Notification.PRIORITY_HIGH);
        NotificationManager manager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1, builder.build());
    }

    protected void showAlertDialog(final SharedPreferences sp, final File file, boolean fileExists, final Activity activity, final String versionName, final IUpdateBean iUpdateBean, final int versionCode) {
        AlertDialog.Builder builder = new AlertDialog
                .Builder(activity)
                .setCancelable(false)
                .setTitle(versionName + " " + IConstants.hintText)
                .setMessage(iUpdateBean.msg)
                .setNegativeButton(IConstants.laterText, new
                        DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        sp.edit()
                                .putLong("time", IUpdateUtils.dayBegin(new Date()).getTime())
                                .putInt("versionCode", versionCode)
                                .putString("versionName", versionName)
                                .apply();
                    }
                });
        if (fileExists) {
            builder.setPositiveButton(IConstants.installText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                                .detectFileUriExposure()
                                .penaltyLog()
                                .build());
                    Uri uri = Uri.fromFile(file);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    //如果没有设置SDCard写权限，或者没有sdcard,apk文件保存在内存中，需要授予权限才能安装
                    String[] command = {"chmod", "777", file.toString()};
                    ProcessBuilder pBuilder = new ProcessBuilder(command);
                    try {
                        pBuilder.start();
                        intent.setDataAndType(uri, "application/vnd.android.package-archive");
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            builder.setPositiveButton(IConstants.downloadText + "(" + IUpdateUtils.formatToMegaBytes(iUpdateBean.size) + "M)",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(activity, IUpdateService.class);
                            intent.putExtra("iUpdateBean", iUpdateBean);
                            intent.putExtra("appIcon", mIconResId);
                            activity.startService(intent);
                        }
                    });
        }
        try {
            builder.show();
        } catch (Exception ignored) {
        }
    }

    public static class Builder {

        protected IUpdateBean updateBeanProvided;
        protected String jsonUrl;
        protected int iconResId;
        protected boolean showDialogIfWifi;

        public Builder setUpdateBean(IUpdateBean updateBeanProvided) {
            this.updateBeanProvided = updateBeanProvided;
            return this;
        }

        public Builder setJsonUrl(String jsonUrl) {
            this.jsonUrl = jsonUrl;
            return this;
        }

        public Builder setIconResId(int iconResId) {
            this.iconResId = iconResId;
            return this;
        }

        public Builder setShowDialogIfWifi(boolean showDialogIfWifi) {
            this.showDialogIfWifi = showDialogIfWifi;
            return this;
        }

        public Builder setDebugMode(boolean debugMode) {
            IConstants.debugMode = debugMode;
            return this;
        }

        public Builder setDownloadText(String downloadText) {
            if (!TextUtils.isEmpty(downloadText)) IConstants.downloadText = downloadText;
            return this;
        }

        public Builder setInstallText(String installText) {
            if (!TextUtils.isEmpty(installText)) IConstants.installText = installText;
            return this;
        }

        public Builder setLaterText(String laterText) {
            if (!TextUtils.isEmpty(laterText)) IConstants.laterText = laterText;
            return this;
        }

        public Builder setHintText(String hintText) {
            if (!TextUtils.isEmpty(hintText)) IConstants.hintText = hintText;
            return this;
        }

        public Builder setDownloadingText(String downloadingText) {
            if (!TextUtils.isEmpty(downloadingText)) IConstants.downloadingText = downloadingText;
            return this;
        }

        public IUpdateApi build() {
            if (sInstance == null) sInstance = new IUpdateApi();
            if (updateBeanProvided != null) {
                sInstance.mUpdateBeanProvided = updateBeanProvided;
            } else {
                sInstance.mJsonUrl = jsonUrl;
            }
            sInstance.mIconResId = iconResId;
            sInstance.mShowDialogIfWifi = showDialogIfWifi;
            return sInstance;
        }
    }
}
