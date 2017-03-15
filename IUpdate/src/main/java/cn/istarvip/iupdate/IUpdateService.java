package cn.istarvip.iupdate;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import cn.istarvip.iupdate.event.EventBusUtils;
import cn.istarvip.iupdate.event.UpdateEvent;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.schedulers.Schedulers;

/**
 * site:istarvip.cn
 * 作者：Administrator on 2017/3/15 11:52
 * 邮箱：917017530@qq.com
 * FIXME 更新服务
 */

public class IUpdateService extends Service {
    protected Notification.Builder mBuilder;
    protected NotificationManager mNotificationManager;
    protected volatile int mFileLength;
    protected volatile int mLength;
    protected DeleteReceiver mDeleteReceiver;
    protected File mFile;
    protected volatile boolean mInterrupted;
    protected Subscription mSubscription;

    protected static final int TYPE_FINISHED = 0;
    protected static final int TYPE_DOWNLOADING = 1;

    protected class DeleteReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            mInterrupted = true;
            handler.sendEmptyMessage(TYPE_FINISHED);
            mNotificationManager.cancel(2);
            if (mFile != null && mFile.exists()) mFile.delete();
            stopSelf();
        }
    }

    @SuppressLint("HandlerLeak")
    protected Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            removeCallbacksAndMessages(null);
            switch (msg.what) {
                case TYPE_DOWNLOADING:
                    if (mInterrupted) {
                        mNotificationManager.cancel(2);
                    } else {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) return;
                        mNotificationManager.notify(2, mBuilder
                                .setContentText(IUpdateUtils.formatToMegaBytes(mLength) + "M/" + IUpdateUtils.formatToMegaBytes(mFileLength) + "M")
                                .setProgress(mFileLength, mLength, false)
                                .build());
                        sendEmptyMessageDelayed(TYPE_DOWNLOADING, 500);
                    }
                    break;
                case TYPE_FINISHED:
                    mNotificationManager.cancel(2);
                    break;
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    Intent mIntent=null;
    @SuppressWarnings("ResourceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final IUpdateBean iUpdateBean = (IUpdateBean) intent.getSerializableExtra("iUpdateBean");
        int iconResId = intent.getIntExtra("appIcon", 0);
        if (iUpdateBean == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        mDeleteReceiver = new DeleteReceiver();
        getApplicationContext().registerReceiver(mDeleteReceiver, new IntentFilter("cn.istarvip.iupdate.delete"));
        int smallIconResId = iconResId > 0 ? iconResId : IUpdateUtils.getAppIconResId(getApplicationContext());
        String title = IUpdateUtils.getApplicationName(getApplicationContext()) + " " + iUpdateBean.versionName + " " + IConstants.downloadingText + "...";
        mBuilder = new Notification.Builder(IUpdateService.this)
                .setProgress(0, 0, false)
                .setAutoCancel(false)
                .setTicker(title)
                .setSmallIcon(smallIconResId)
                .setContentTitle(title)
                .setContentText("");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBuilder.setShowWhen(true);
            mBuilder.setVibrate(new long[0]);
            mBuilder.setPriority(Notification.PRIORITY_HIGH);
        }
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mSubscription = Observable.create(new Observable.OnSubscribe<Response>() {

            @Override
            public void call(Subscriber<? super Response> subscriber) {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(iUpdateBean.url).build();
                Response response;
                try {
                    response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        subscriber.onNext(response);
                    } else {
                        subscriber.onError(new IOException(response.code() + ": " + response.body().string()));
                    }
                } catch (Throwable t) {
                    subscriber.onError(t);
                }
            }
        }).subscribeOn(Schedulers.io()).subscribe(new Subscriber<Response>() {
            @Override
            public void onCompleted() {

            }
            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                EventBusUtils.post(new UpdateEvent(2,IConstants.downloadError));
            }

            @Override
            public void onNext(Response response) {
                InputStream is = null;
                FileOutputStream fos = null;
                try {
                    is = response.body().byteStream();
                    mFileLength = (int) response.body().contentLength();
                    mFile = new File(getExternalCacheDir(), "update.apk");
                    if (mFile.exists()) mFile.delete();
                    fos = new FileOutputStream(mFile);
                    byte[] buffer = new byte[8192];
                    int hasRead;
                    handler.sendEmptyMessage(TYPE_DOWNLOADING);
                    mInterrupted = false;
                    while ((hasRead = is.read(buffer)) >= 0) {
                        if (mInterrupted) return;
                        fos.write(buffer, 0, hasRead);
                        mLength = mLength + hasRead;
                    }
                    handler.sendEmptyMessage(TYPE_FINISHED);
                    mLength = 0;
                    if (mFile.exists()) {
                        String md5JustDownloaded = IUpdateUtils.getMd5ByFile(mFile);
                        String md5InUpdateBean = iUpdateBean.md5;
                        if (md5JustDownloaded.equalsIgnoreCase(md5InUpdateBean)) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                                        .detectFileUriExposure()
                                        .penaltyLog()
                                        .build());
                            Uri uri = Uri.fromFile(mFile);
                            mIntent = new Intent(Intent.ACTION_VIEW);
                            //如果没有设置SDCard写权限，或者没有sdcard,apk文件保存在内存中，需要授予权限才能安装
                            String[] command = {"chmod", "777", mFile.toString()};
                            ProcessBuilder builder = new ProcessBuilder(command);
                            try {
                                builder.start();
                                Log.e("aaaaaaaaaaaaaaa", uri.toString());
                                mIntent.setDataAndType(uri, "application/vnd.android.package-archive");
                                mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(mIntent);
                            } catch (Exception e) {
                                Log.e("aaaaaaaaa", "file路径错误");
                                e.printStackTrace();
                            }
                        } else {
                            mFile.delete();
                            EventBusUtils.post(new UpdateEvent(2,IConstants.md5Error));
                            throw new Exception("MD5 mismatch. md5JustDownloaded: " + md5JustDownloaded + ". md5InUpdateBean: " + md5InUpdateBean + ".");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    EventBusUtils.post(new UpdateEvent(2,IConstants.downloadError));
                    sendBroadcast(new Intent("cn.istarvip.iupdate.delete"));
                } finally {
                    IUpdateUtils.closeQuietly(fos);
                    IUpdateUtils.closeQuietly(is);
                    stopSelf();
                }
            }
        });
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDeleteReceiver != null) getApplicationContext().unregisterReceiver(mDeleteReceiver);
        if (mSubscription != null) mSubscription.unsubscribe();
    }
}
