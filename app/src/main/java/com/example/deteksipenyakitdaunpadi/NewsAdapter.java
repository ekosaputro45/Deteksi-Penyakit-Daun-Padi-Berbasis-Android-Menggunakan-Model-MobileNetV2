package com.example.deteksipenyakitdaunpadi;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(NewsItem item);
    }

    private final Context context;
    private final List<NewsItem> items;
    private final OnItemClickListener listener;

    public NewsAdapter(Context context, List<NewsItem> items, OnItemClickListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsItem item = items.get(position);
        holder.tvTitle.setText(item.title);
        holder.tvMeta.setText(item.source + " • " + item.date);
        if (item.summary != null && !item.summary.trim().isEmpty()) {
            holder.tvSummary.setVisibility(View.VISIBLE);
            holder.tvSummary.setText(item.summary);
        } else {
            holder.tvSummary.setText("");
            holder.tvSummary.setVisibility(View.GONE);
        }

        if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            try {
                Glide.with(context)
                        .load(Uri.parse(item.imageUrl))
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .centerCrop()
                        .into(holder.ivThumb);
            } catch (Exception e) {
                holder.ivThumb.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            holder.ivThumb.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvTitle;
        TextView tvMeta;
        TextView tvSummary;

        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.ivNewsThumb);
            tvTitle = itemView.findViewById(R.id.tvNewsTitle);
            tvMeta = itemView.findViewById(R.id.tvNewsMeta);
            tvSummary = itemView.findViewById(R.id.tvNewsSummary);
        }
    }
}
