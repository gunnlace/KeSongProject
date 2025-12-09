package com.example.kesongproject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

    /**
     * 将时间戳转换为友好的显示格式
     * @param timestampSeconds 服务器返回的时间戳 (秒)
     * @return 格式化后的字符串
     */
    public static String getFriendlyTimeSpanByNow(long timestampSeconds) {
        long now = System.currentTimeMillis();
        long target = timestampSeconds * 1000; // 转毫秒
        long diff = now - target;

        // 获取 Calendar 对象方便判断日期
        Calendar nowCal = Calendar.getInstance();
        nowCal.setTimeInMillis(now);

        Calendar targetCal = Calendar.getInstance();
        targetCal.setTimeInMillis(target);

        // 格式化工具
        SimpleDateFormat fmtHourMin = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat fmtMonthDay = new SimpleDateFormat("MM-dd", Locale.getDefault());

        // 1. 判断是否是 24小时内
        // (这里简化逻辑：如果是今天或者昨天，且时间差在24h内)
        long oneDayMillis = 24 * 60 * 60 * 1000L;

        if (diff < oneDayMillis && diff >= 0) {
            // 是今天吗？
            if (isSameDay(nowCal, targetCal)) {
                return fmtHourMin.format(new Date(target)); // "14:30"
            }
            // 是昨天吗？
            else if (isYesterday(nowCal, targetCal)) {
                return "昨天 " + fmtHourMin.format(new Date(target)); // "昨天 14:30"
            }
        }

        // 2. 判断是否是 7天内
        long sevenDaysMillis = 7 * oneDayMillis;
        if (diff < sevenDaysMillis && diff > 0) {
            int days = (int) (diff / oneDayMillis);
            // 避免除法算出 0 天的情况（其实0天已经被上面24h捕获了，这里兜底显示1天前）
            if (days <= 0) days = 1;
            return days + "天前";
        }

        // 3. 其余显示具体日期
        return fmtMonthDay.format(new Date(target)); // "03-15"
    }

    // 辅助方法：判断是不是同一天
    private static boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    // 辅助方法：判断 cal2 是不是 cal1 的昨天
    private static boolean isYesterday(Calendar current, Calendar target) {
        // 克隆一个 current，减一天，看是不是和 target 同一天
        Calendar yesterday = (Calendar) current.clone();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        return isSameDay(yesterday, target);
    }
}