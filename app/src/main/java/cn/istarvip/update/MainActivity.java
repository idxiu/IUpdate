package cn.istarvip.update;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.greenrobot.eventbus.Subscribe;

import cn.istarvip.iupdate.IConstants;
import cn.istarvip.iupdate.IUpdateApi;
import cn.istarvip.iupdate.IUpdateUtils;
import cn.istarvip.iupdate.event.EventBusUtils;
import cn.istarvip.iupdate.event.UpdateEvent;

public class MainActivity extends AppCompatActivity {
    Button btn;
    private Activity activity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn = (Button) findViewById(R.id.btn);
        //注册事件
        EventBusUtils.register(this);
        final IUpdateApi updateApi = new IUpdateApi
                .Builder()
                .setDebugMode(false)    //是否显示调试信息(可选,默认:false)
                //  .setUpdateBean(updateBean) //设置通过其他途径得到的XdUpdateBean(2选1)
                .setJsonUrl("http://192.168.8.103:8080/a/a.xml")   //JSON文件的URL(2选1)
                .setShowDialogIfWifi(true)  //设置在WiFi下直接弹出AlertDialog而不使用Notification(可选,默认:false)
                .setDownloadText("立即下载")    //可选,默认为左侧所示的文本
                .setInstallText("立即安装(已下载)")
                .setLaterText("以后再说")
                .setHintText("版本更新")
                .setDownloadingText("正在下载")
                .setIconResId(R.mipmap.ic_launcher) //设置在通知栏显示的通知图标资源ID(可选,默认为应用图标)
                .build();
        if (IUpdateUtils.isConnected(activity)) {
            updateApi.update(activity);
        } else {
            EventBusUtils.post(new UpdateEvent(2, IConstants.notNet));
        }
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (IUpdateUtils.isConnected(activity)) {
                    updateApi.forceUpdate(activity);
                } else {
                    EventBusUtils.post(new UpdateEvent(2, IConstants.notNet));
                }
            }
        });
    }
    //订阅事件
    @Subscribe
    public void onUpdateEvent(UpdateEvent event) {
        Log.e("aaaaaaaa", event.toString());
        switch (event.flg) {
            case 1:
                //success
                boolean flg=event.needUpdate;
                if (flg) {
                    //需要更新
                } else {
                    //最新版本
                }
                Log.e("aaaaaaaaaaaa","是否需要更新-->"+flg);
                break;
            case 2:
                //fail
                switch (event.code) {
                    case IConstants.notNet:
                        Log.e("aaaaaaaaaaaa","没有网络");
                        break;
                    case IConstants.requestError:
                        Log.e("aaaaaaaaaaaa","请求失败");
                        break;
                    case IConstants.jsonError:
                        Log.e("aaaaaaaaaaaa","json配置异常");
                        break;
                    case IConstants.md5Error:
                        Log.e("aaaaaaaaaaaa","MD5加密异常");
                        break;
                    case IConstants.downloadError:
                        Log.e("aaaaaaaaaaaa","下载失败");
                        break;
                }

                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //取消
        EventBusUtils.unregister(this);
    }
}
