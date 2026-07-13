package com.wzm.mydemo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * 已安装应用列表适配器
 */
public class InstalledAppsAdapter extends BaseAdapter {

    private final Context context;// 上下文
    private final List<AppInfo> apps;// 已安装应用数据列表
    private final PackageManager pm;// 包管理器

    public InstalledAppsAdapter(Context context, List<AppInfo> apps) {
        this.context = context;
        this.apps = apps;
        this.pm = context.getPackageManager();
    }

    @Override
    public int getCount() {
        return apps.size();
    }

    @Override
    public AppInfo getItem(int position) {
        return apps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    // 返回指定位置的 View
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_installed_app, parent, false);
            holder = new ViewHolder();
            holder.icon = convertView.findViewById(R.id.iv_app_icon);
            holder.name = convertView.findViewById(R.id.tv_app_name);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        AppInfo app = apps.get(position);
        holder.icon.setImageDrawable(app.icon);
        holder.name.setText(app.appName);

        // 焦点缩放动画
        convertView.setOnFocusChangeListener((v, hasFocus) -> {
            v.animate().cancel();
            float s = hasFocus ? 1.08f : 1.0f;
            v.animate().scaleX(s).scaleY(s).setDuration(150).start();
        });

        // 点击启动对应应用
        convertView.setOnClickListener(v -> {
            Intent intent = pm.getLaunchIntentForPackage(app.packageName);
            if (intent != null) context.startActivity(intent);
        });

        return convertView;
    }

    static class ViewHolder {
        ImageView icon;
        TextView name;
    }
}