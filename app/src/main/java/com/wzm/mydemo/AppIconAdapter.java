package com.wzm.mydemo;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 中部应用快捷图标适配器
 */
public class AppIconAdapter extends RecyclerView.Adapter<AppIconAdapter.VH> {

    // 图标点击监听接口
    public interface OnIconClickListener {
        void onIconClick(int position, String name);
    }

    private static final String[] NAMES = {"Netflix", "YouTube", "Google Play", "Chrome"};//4个应用图标名称
    private static final int[] COLORS = {0xFF000000, 0xFF333333, 0xFFA8E6A3, 0xFF7B1FA2};//4个应用图标背景色
    private static final int[] ICONS = {
            R.drawable.ic_netflix,
            R.drawable.ic_youtube,
            R.drawable.ic_google_play,
            R.drawable.ic_chrome
    };//4个应用图标图片资源

    private OnIconClickListener listener;// 点击监听器引用

    // 设置点击监听器
    public void setOnIconClickListener(OnIconClickListener l) {
        this.listener = l;
    }

    // 创建 ViewHolder
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_icon, parent, false);
        return new VH(v);
    }

    // 绑定数据到 ViewHolder
    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.label.setText(NAMES[position]);// 设置应用名称

        float density = holder.itemView.getContext().getResources().getDisplayMetrics().density;

        // 动态创建圆角矩形背景 Drawable
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setShape(GradientDrawable.RECTANGLE);
        cardBg.setCornerRadius((int) (14 * density));
        cardBg.setColor(COLORS[position]);
        holder.iconCard.setBackground(cardBg);

        // 加载并设置应用图标
        Drawable drawable = holder.itemView.getContext().getDrawable(ICONS[position]);
        holder.ivIcon.setImageDrawable(drawable);

        // 在 icon_card 布局完成后，将其截图传给 ReflectionImageView 生成倒影
        holder.iconCard.post(() -> {
            holder.reflectionView.setSourceView(holder.iconCard);
        });

        // 焦点变化监听
        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            v.animate().cancel();
            float s = hasFocus ? 1.12f : 1.0f;
            v.animate().scaleX(s).scaleY(s).setDuration(200).start();
            v.setElevation(hasFocus ? 20 : 0);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onIconClick(position, NAMES[position]);
        });

        holder.itemView.setNextFocusDownId(R.id.bottom_bar);
    }

    @Override
    public int getItemCount() {
        return NAMES.length;
    }

    // ViewHolder 内部类
    static class VH extends RecyclerView.ViewHolder {
        ImageView ivIcon;// 应用图标
        TextView label;// 应用名称
        LinearLayout iconCard;// 图标卡片容器
        ReflectionImageView reflectionView;// 反射倒影 View

        VH(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            label = itemView.findViewById(R.id.tv_icon_label);
            iconCard = itemView.findViewById(R.id.icon_card);
            reflectionView = itemView.findViewById(R.id.reflection_view);
        }
    }
}
