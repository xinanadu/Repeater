package info.zhegui.repeater;

/**
 * Created by Administrator on 2015/8/22.
 */
public class Utils {
    public String formatTime(long mm) {
        int m = (int) mm / (60000);
        int s = (int) (mm - m * 60000) / 1000;

        return (m < 10 ? ("0" + m) : m) + ":" + (s < 10 ? ("0" + s) : s);
//            return getResources().getString(R.string.default_time);
    }
}
