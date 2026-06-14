package com.example.bubblepet;

import android.app.ActivityManager;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;

import java.util.List;

/**
 * 检测当前前台 app，判断是桌面启动器还是其他 app。
 *
 * 需要 PACKAGE_USAGE_STATS 权限（特殊权限，用户在设置里授权）。
 */
public final class ForegroundAppDetector {

    private final Context context;
    private final UsageStatsManager usageStatsManager;
    private final String launcherPkg;

    public ForegroundAppDetector(Context context) {
        this.context = context.getApplicationContext();
        this.usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        this.launcherPkg = resolveLauncherPackage();
    }

    /**
     * 查询默认启动器包名（用于判断是否在桌面）。
     */
    private String resolveLauncherPackage() {
        try {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            PackageManager pm = context.getPackageManager();
            ResolveInfo info = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (info != null && info.activityInfo != null) {
                return info.activityInfo.packageName;
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    /**
     * 检查是否已获得 PACKAGE_USAGE_STATS 权限。
     */
    public boolean hasPermission() {
        try {
            return android.app.AppOpsManager.MODE_ALLOWED ==
                    ((android.app.AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE))
                            .checkOpNoThrow(
                                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                                    android.os.Process.myUid(),
                                    context.getPackageName());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断当前前台是否为桌面启动器。
     * 无权限时返回 false（按"非桌面"处理，安全降级）。
     */
    public boolean isOnLauncher() {
        if (!hasPermission()) {
            // 无权限时默认按桌面处理，避免宠物被错误限制
            return true;
        }
        String pkg = getForegroundPackage();
        if (pkg == null || launcherPkg == null) return true;
        return launcherPkg.equals(pkg);
    }

    /**
     * 获取当前前台 app 包名。
     * 优先用 ActivityManager.getRunningAppProcesses()（实时），
     * 回退到 UsageStatsManager（有延迟）。
     */
    private String getForegroundPackage() {
        // 方案1: ActivityManager（实时，但部分 ROM 受限）
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
                if (processes != null) {
                    for (ActivityManager.RunningAppProcessInfo p : processes) {
                        if (p.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                            String pkg = p.processName;
                            // 排除自己的 service 进程
                            if (!pkg.equals(context.getPackageName())) {
                                return pkg;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }

        // 方案2: UsageStatsManager（有延迟，作为回退）
        if (usageStatsManager == null) return null;
        long now = System.currentTimeMillis();
        List<android.app.usage.UsageStats> stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                now - 10_000,
                now);
        if (stats == null || stats.isEmpty()) return null;
        android.app.usage.UsageStats latest = null;
        for (android.app.usage.UsageStats s : stats) {
            if (latest == null || s.getLastTimeUsed() > latest.getLastTimeUsed()) {
                latest = s;
            }
        }
        return latest != null ? latest.getPackageName() : null;
    }

    /**
     * 跳转到使用情况访问权限设置页。
     */
    public static void openUsageAccessSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
