package com.example.deteksipenyakitdaunpadi;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

// Model Class diperbarui
class HistoryItem {
    String id;
    String penyakitName;
    String date;
    String confidence;
    String imagePath; // Field baru

    // Constructor diperbarui
    public HistoryItem(String id, String penyakitName, String date, String confidence, String imagePath) {
        this.id = id;
        this.penyakitName = penyakitName;
        this.date = date;
        this.confidence = confidence;
        this.imagePath = imagePath;
    }
}

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final Context context;
    private final List<HistoryItem> historyItems;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(HistoryItem item);
        void onDeleteClick(HistoryItem item, int position);
    }

    public HistoryAdapter(Context context, List<HistoryItem> historyItems, OnItemClickListener listener) {
        this.context = context;
        this.historyItems = historyItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        HistoryItem item = historyItems.get(position);

        holder.tvPenyakitName.setText(item.penyakitName);
        holder.tvScanDate.setText(item.date);
        holder.tvConfidence.setText(item.confidence);

        // Menggunakan Glide untuk memuat gambar dari path
        if (item.imagePath != null && !item.imagePath.isEmpty()) {
            try {
                Uri imageUri = Uri.parse(item.imagePath);
                Glide.with(context)
                     .load(imageUri)
                     .placeholder(android.R.drawable.ic_menu_gallery)
                     .error(android.R.drawable.ic_menu_report_image)
                     .centerCrop()
                     .into(holder.ivHistoryImage);
            } catch (Exception e) {
                // Jika error, tampilkan placeholder
                holder.ivHistoryImage.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        } else {
            // Jika imagePath kosong, tampilkan placeholder
            holder.ivHistoryImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    public void removeItem(int position) {
        historyItems.remove(position);
        notifyItemRemoved(position);
    }

    public HistoryItem getItem(int position) {
        return historyItems.get(position);
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ConstraintLayout viewForeground;
        ImageView ivHistoryImage;
        TextView tvPenyakitName, tvScanDate, tvConfidence;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            viewForeground = itemView.findViewById(R.id.view_foreground);
            ivHistoryImage = itemView.findViewById(R.id.ivHistoryImage);
            tvPenyakitName = itemView.findViewById(R.id.tvHistoryPenyakitName);
            tvScanDate = itemView.findViewById(R.id.tvScanDate);
            tvConfidence = itemView.findViewById(R.id.tvHistoryConfidence);
        }
    }
}
