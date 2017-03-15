package cn.istarvip.iupdate;

import java.io.Serializable;

/**
 * site:istarvip.cn
 * 作者：Administrator on 2017/3/15 11:51
 * 邮箱：917017530@qq.com
 * FIXME 更新实体
 */

public class IUpdateBean implements Serializable {
    public int versionCode, size;
    public String versionName, url, msg, md5;

    @Override
    public String toString() {
        return "IUpdateBean{" +
                "versionCode=" + versionCode +
                ", size=" + size +
                ", versionName='" + versionName + '\'' +
                ", url='" + url + '\'' +
                ", msg='" + msg + '\'' +
                ", md5='" + md5 + '\'' +
                '}';
    }
}
