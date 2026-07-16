package com.wzm.mydemo;

import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * 桌面界面（主Activity）
 */
public class MainActivity extends AppCompatActivity {

    private TextView tvTime;
    private TextView tvDate;
    private List<AppInfo> installedApps;
    private Thread clockThread;
    private static final TimeZone CHINA_TZ = TimeZone.getTimeZone("GMT+8");
    private AppIconAdapter appIconAdapter;

    //启动时钟更新线程
    private void startClock() {
        if (clockThread != null) return;
        updateClock();
        //创建新线程，每秒更新时间
        clockThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(1000);
                        //在UI线程更新时钟
                        tvTime.post(new Runnable() {
                            @Override
                            public void run() {
                                updateClock();
                            }
                        });
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });
        clockThread.start();
    }

    //停止时钟线程
    private void stopClock() {
        if (clockThread != null) {
            clockThread.interrupt();
            clockThread = null;
        }
    }

    //更新时钟
    private void updateClock() {
        try {
            Date now = new Date();
            SimpleDateFormat timeFmt = new SimpleDateFormat("h:mm a", Locale.US);
            timeFmt.setTimeZone(CHINA_TZ);
            tvTime.setText(timeFmt.format(now));
            SimpleDateFormat dateFmt = new SimpleDateFormat("EEEE, MMMM d", Locale.US);
            dateFmt.setTimeZone(CHINA_TZ);
            tvDate.setText(dateFmt.format(now));
        } catch (Exception ignored) {
        }
    }

    //底部导航图标资源ID数组
    private static final int[] BOTTOM_ICONS = {
            R.drawable.ic_keystone,
            R.drawable.ic_miracast,
            R.drawable.ic_signal_source,
            R.drawable.ic_my_apps,
            R.drawable.ic_settings
    };

    //底部导航标签文字数组
    private static final String[] BOTTOM_LABELS = {
            "Keystone", "Miracast", "Signal Source", "My Apps", "Settings"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enterFullScreen();
        setContentView(R.layout.activity_main);

        tvTime = findViewById(R.id.tv_time);
        tvDate = findViewById(R.id.tv_date);

        startClock();
        setupAppIcons();
        setupBottomBar();
        loadInstalledApps();

        if (!hasUsagePermission()) {
            Toast.makeText(this, "请授权使用情况访问权限以显示常用应用", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
    }

    //进入沉浸式全屏模式
    private void enterFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    //设置快捷应用图标
    private void setupAppIcons() {
        RecyclerView rv = findViewById(R.id.rv_app_icons);
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        appIconAdapter = new AppIconAdapter();
        rv.setAdapter(appIconAdapter);

        appIconAdapter.setOnIconClickListener((position, name) -> showAllAppsDialog());

        if (hasUsagePermission()) {
            loadFrequentApps();
        } else {
            loadFrequentFallback();
        }
    }

    private boolean hasUsagePermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void loadFrequentApps() {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
        long end = System.currentTimeMillis();
        long start = end - 1000L * 60 * 60 * 24 * 7;

        Map<String, UsageStats> statsMap = usm.queryAndAggregateUsageStats(start, end);
        List<UsageStats> stats = new ArrayList<>(statsMap.values());
        stats.sort((a, b) -> Long.compare(b.getTotalTimeInForeground(), a.getTotalTimeInForeground()));

        PackageManager pm = getPackageManager();
        String myPackage = getPackageName();
        List<AppInfo> frequentApps = new ArrayList<>();

        for (UsageStats stat : stats) {
            if (frequentApps.size() >= 4) break;
            String pkg = stat.getPackageName();
            if (pkg.equals(myPackage)) continue;
            try {
                ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                AppInfo info = new AppInfo();
                info.appName = ai.loadLabel(pm).toString();
                info.packageName = pkg;
                info.icon = ai.loadIcon(pm);
                frequentApps.add(info);
            } catch (Exception ignored) {
            }
        }

        appIconAdapter.setData(frequentApps);
    }

    private void loadFrequentFallback() {
        if (installedApps == null || installedApps.isEmpty()) {
            loadInstalledApps();
        }
        List<AppInfo> top4 = new ArrayList<>();
        for (int i = 0; i < Math.min(4, installedApps.size()); i++) {
            top4.add(installedApps.get(i));
        }
        appIconAdapter.setData(top4);
    }

    //设置底部导航栏
    private void setupBottomBar() {
        LinearLayout bottomBar = findViewById(R.id.bottom_bar);
        float density = getResources().getDisplayMetrics().density;
        int spacing = (int) (14 * density);

        //循环创建5个底部导航项
        for (int i = 0; i < BOTTOM_ICONS.length; i++) {
            View itemView = getLayoutInflater().inflate(R.layout.item_bottom_nav, bottomBar, false);

            ImageView iv = itemView.findViewById(R.id.iv_bottom_icon);
            TextView tv = itemView.findViewById(R.id.tv_bottom_label);
            iv.setImageResource(BOTTOM_ICONS[i]);
            tv.setText(BOTTOM_LABELS[i]);

            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) itemView.getLayoutParams();
            lp.width = 0;
            lp.weight = 1;
            if (i < BOTTOM_ICONS.length - 1) {
                lp.setMarginEnd(spacing);
            }
            itemView.setLayoutParams(lp);

            final int index = i;
            itemView.setOnClickListener(v -> onBottomItemClick(index));
            itemView.setNextFocusUpId(R.id.rv_app_icons);

            itemView.setOnFocusChangeListener((v, hasFocus) -> {
                v.animate().cancel();
                float s = hasFocus ? 1.08f : 1.0f;
                v.animate().scaleX(s).scaleY(s).setDuration(150).start();
            });

            bottomBar.addView(itemView);
        }

        //设置左右焦点导航
        for (int i = 0; i < bottomBar.getChildCount(); i++) {
            View child = bottomBar.getChildAt(i);
            if (i > 0) {
                child.setNextFocusLeftId(bottomBar.getChildAt(i - 1).getId());
            }
            if (i < bottomBar.getChildCount() - 1) {
                child.setNextFocusRightId(bottomBar.getChildAt(i + 1).getId());
            }
        }
    }

    //处理底部栏点击事件
    private void onBottomItemClick(int index) {
        switch (index) {
            case 0:
                Toast.makeText(this, "Keystore", Toast.LENGTH_SHORT).show();
                break;
            case 1:
                Toast.makeText(this, "Miracast", Toast.LENGTH_SHORT).show();
                break;
            case 2:
                Toast.makeText(this, "Signal Source", Toast.LENGTH_SHORT).show();
                break;
            case 3:
                showAllAppsDialog();
                break;
            case 4:
                startActivity(new Intent(Settings.ACTION_SETTINGS));
                break;
        }
    }

    //加载已安装应用
    private void loadInstalledApps() {
        installedApps = new ArrayList<>();
        PackageManager pm = getPackageManager();
        String myPackage = getPackageName();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(mainIntent, 0);

        //遍历每个应用，跳过自身
        for (ResolveInfo ri : list) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(myPackage)) continue;
            ApplicationInfo ai = ri.activityInfo.applicationInfo;
            AppInfo info = new AppInfo();
            info.appName = ai.loadLabel(pm).toString();
            info.packageName = pkg;
            info.icon = ai.loadIcon(pm);
            installedApps.add(info);
        }
    }

    //显示所有已安装应用的对话框
    private void showAllAppsDialog() {
        if (installedApps == null || installedApps.isEmpty()) {
            loadInstalledApps();
        }

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_all_apps);

        GridView gv = dialog.findViewById(R.id.gv_all_apps);
        gv.setAdapter(new InstalledAppsAdapter(this, installedApps));

        gv.setOnItemClickListener((parent, v, position, id) -> {
            AppInfo app = installedApps.get(position);
            dialog.dismiss();
            Intent intent = getPackageManager().getLaunchIntentForPackage(app.packageName);
            if (intent != null) startActivity(intent);
        });

        dialog.setCancelable(true);
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
            window.setGravity(Gravity.CENTER);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        enterFullScreen();
        startClock();
        if (hasUsagePermission()) {
            loadFrequentApps();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopClock();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enterFullScreen();
            startClock();
        }
    }

    //处理按键按下事件
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            showAllAppsDialog();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
