package cn.istarvip.iupdate.event;

import cn.istarvip.iupdate.IUpdateBean;

/**
 * site:istarvip.cn
 * 作者：Administrator on 2017/3/15 12:30
 * 邮箱：917017530@qq.com
 * FIXME
 */

public class UpdateEvent {
    public int flg;
    public int code;
    public boolean needUpdate;
    public IUpdateBean updateBean;
    //请求错误
    public UpdateEvent(int flg, int code) {
        this.flg = flg;
        this.code = code;
    }

    public UpdateEvent(int flg) {
        this.flg = flg;
    }
    //1 更新
    public UpdateEvent(int flg, boolean needUpdate, IUpdateBean updateBean) {
        this.flg = flg;
        this.needUpdate = needUpdate;
        this.updateBean = updateBean;
    }

    @Override
    public String toString() {
        return "UpdateEvent{" +
                "flg=" + flg +
                ", code=" + code +
                ", needUpdate=" + needUpdate +
                ", updateBean=" + updateBean +
                '}';
    }
}
