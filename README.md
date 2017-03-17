# IUpdate 安卓自动检测更新工具
init 1.0
## 1.准备描述更新信息的JSON文件
```
  {
  "versionCode":4,                          //新版本的versionCode,int型
  "versionName":"1.12",                     //新版本的versionName,String型
  "url":"http://contoso.com/app.apk",       //APK下载地址,String型
  "msg":"Bug修复",                         //更新内容,String型
  "md5":"D23788B6A1F95C8B6F7E442D6CA7536C", //32位MD5值,String型
  "size":17962350                           //大小(字节),int型
  }
```
## 2.构建IUpdateApi对象
```
        //注册事件
        EventBusUtils.register(this);
        final IUpdateApi updateApi = new IUpdateApi
                .Builder()
                .setDebugMode(false)    //是否显示调试信息(可选,默认:false)
              //.setUpdateBean(updateBean) //设置通过其他途径得到的IUpdateBean(2选1)
                .setJsonUrl("http://192.168.8.103:8080/a/a.xml")   //JSON文件的URL(2选1)
                .setShowDialogIfWifi(true)  //设置在WiFi下直接弹出AlertDialog而不使用Notification(可选,默认:false)
                .setDownloadText("立即下载")    //可选,默认为左侧所示的文本
                .setInstallText("立即安装(已下载)")
                .setLaterText("以后再说")
                .setHintText("版本更新")
                .setDownloadingText("正在下载")
                .setIconResId(R.mipmap.ic_launcher) //设置在通知栏显示的通知图标资源ID(可选,默认为应用图标)
                .build();
```
## 3.检查更新
```
  if (IUpdateUtils.isConnected(activity)) {
          updateApi.update(activity);
   } else {
          EventBusUtils.post(new UpdateEvent(2, IConstants.notNet));
   }
  
```
适用于 App 入口的自动检查更新。默认策略下：

1.若用户选择“以后再说”或者划掉了通知栏的更新提示，则当天对该版本不再提示更新，防止当天每次打开应用时都提示导致用户不胜其烦；

2.在任何网络环境下，均推送一条通知栏更新提示，点击通知后弹出对话框，防止直接弹框带来不好的用户体验。

可调用 IUpdateApi.Builder.setShowDialogIfWifi(true) 设置在 WiFi 下直接弹出更新提示框 (AlertDialog) 而不使用 Notification 的形式。
```
  if (IUpdateUtils.isConnected(activity)) {
       updateApi.forceUpdate(activity);
   } else {
       EventBusUtils.post(new UpdateEvent(2, IConstants.notNet));
   }
 
```
适用于应用“设置”页面的手动检查更新。此方法无视上面的 2 条默认策略，如果有更新，总是对用户进行提示，且总是使用提示框 (AlertDialog) 的形式。

## 4.若不想使用JSON文件，可传入由其他途径得到的IUpdateBean 
```
  IUpdateApi.Builder.setUpdateBean(IUpdateApi updateBean);
```
可使用第三方推送服务的自定义消息/透传功能，接收到服务端推送过来的JSON(String)后，解析成一个XdUpdateBean，传入上述方法，即可使用推送带过来的JSON进行更新提示。

注意不是普通消息，这样会直接在通知栏上显示内容，不会进到自定义的代码处理块。
## 5.更新监听：

```
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
  ```
## 6.other：
#### eventbus
#### okhttp3 
#### okio
#### rxandroid
#### rxjava
#### gson    
