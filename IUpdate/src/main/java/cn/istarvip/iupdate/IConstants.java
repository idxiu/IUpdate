package cn.istarvip.iupdate;

import java.io.Serializable;

/**
 * site:istarvip.cn
 * 作者：Administrator on 2017/3/15 11:50
 * 邮箱：917017530@qq.com
 * FIXME 弹窗
 */
public class IConstants implements Serializable {

    protected static boolean debugMode = false;
    protected static String downloadText = "立即下载";
    protected static String installText = "立即安装(已下载)";
    protected static String laterText = "以后再说";
    protected static String hintText = "版本更新";
    protected static String downloadingText = "正在下载";

    /*************************监听事件************************************/
    public final static int  notNet =0xf0002;
    public final static int  requestError =0xf0003;
    public final static int  jsonError =0xf0004;
    public final static int  md5Error =0xf0005;
    public final static int  downloadError =0xf0006;



}
