package com.wzm.mydemo;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AppIconAdapter extends RecyclerView.Adapter<AppIconAdapter.VH> {

    public interface OnIconClickListener {
        void onIconClick(int position, String name);
    }

    private static final int[] FALLBACK_COLORS = {
            0xFF000000, 0xFF333333, 0xFFA8E6A3, 0xFF7B1FA2
    };

    private final List<AppInfo> apps = new ArrayList<>();
    private OnIconClickListener listener;

    public void setData(List<AppInfo> newApps) {
        apps.clear();
        if (newApps != null) {
            apps.addAll(newApps);
        }
        notifyDataSetChanged();
    }

    public void setOnIconClickListener(OnIconClickListener l) {
        this.listener = l;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_icon, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        AppInfo app = apps.get(position);
        holder.label.setText(app.appName);

        float density = holder.itemView.getContext().getResources().getDisplayMetrics().density;

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setShape(GradientDrawable.RECTANGLE);
        cardBg.setCornerRadius((int) (14 * density));
        cardBg.setColor(FALLBACK_COLORS[position % FALLBACK_COLORS.length]);
        holder.iconCard.setBackground(cardBg);

        holder.ivIcon.setImageDrawable(app.icon);

        holder.iconCard.post(() -> holder.reflectionView.setSourceView(holder.iconCard));

        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            v.animate().cancel();
            float s = hasFocus ? 1.12f : 1.0f;
            v.animate().scaleX(s).scaleY(s).setDuration(200).start();
            v.setElevation(hasFocus ? 20 : 0);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onIconClick(position, app.appName);
        });

        holder.itemView.setNextFocusDownId(R.id.bottom_bar);
    }

    public AppInfo getItem(int position) {
        return apps.get(position);
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView label;
        LinearLayout iconCard;
        ReflectionImageView reflectionView;

        VH(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            label = itemView.findViewById(R.id.tv_icon_label);
            iconCard = itemView.findViewById(R.id.icon_card);
            reflectionView = itemView.findViewById(R.id.reflection_view);
        }
    }
}
