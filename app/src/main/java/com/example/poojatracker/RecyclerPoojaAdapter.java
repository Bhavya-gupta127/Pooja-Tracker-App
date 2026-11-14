package com.example.poojatracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class RecyclerPoojaAdapter extends RecyclerView.Adapter<RecyclerPoojaAdapter.ViewHolder> {
    Context context;
    ArrayList<PoojaModel> arrPooja;
    SelectListener listener;

    RecyclerPoojaAdapter(Context context, ArrayList<PoojaModel> arrPooja, SelectListener listener) {
        this.context = context;
        this.arrPooja = arrPooja;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.pooja_row, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("RecyclerView")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PoojaModel item = arrPooja.get(position);
        String thumbnail = "https://img.youtube.com/vi/" + item.VideoId + "/mqdefault.jpg";
        try {
            Picasso.get()
                    .load(thumbnail)
                    .placeholder(R.drawable.omicon)
                    .error(R.drawable.omicon)
                    .into(holder.img);
        } catch (Exception e) {
            Log.w("imageissue", "Thumbnail load failed", e);
        }

        boolean isDone = item.status;
        holder.status.setChecked(isDone);

        String cleanTitle = item.title != null ? item.title.trim() : "";
        String cleanDesc = item.desc != null ? item.desc.trim() : "";
        holder.title.setText(cleanTitle);
        holder.contentURL.setText(item.contentURL);

        if (cleanDesc.isEmpty()) {
            holder.desc.setVisibility(View.GONE);
        } else {
            holder.desc.setVisibility(View.VISIBLE);
            holder.desc.setText(cleanDesc);
        }

        if (isDone) {
            holder.title.setPaintFlags(holder.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.title.setAlpha(0.72f);
            holder.rowPlayHint.setText("✓ Completed");
            holder.rowPlayHint.setTextColor(ContextCompat.getColor(context, R.color.badge_done_text));
        } else {
            holder.title.setPaintFlags(holder.title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.title.setAlpha(1.0f);
            holder.rowPlayHint.setText("▶ Tap to play");
            holder.rowPlayHint.setTextColor(ContextCompat.getColor(context, R.color.accent_link));
        }

        holder.cardview.setOnClickListener(v -> listener.onItemClicked(item));
        holder.status.setOnClickListener(v -> listener.onCheckboxClicked(item));
        holder.btnDeleteRow.setOnClickListener(v -> listener.onDeleteClicked(item));
    }

    @Override
    public int getItemCount() {
        return arrPooja.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, desc, contentURL, rowPlayHint, btnDeleteRow;
        CheckBox status;
        ImageView img;
        CardView cardview;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.pooja_name);
            desc = itemView.findViewById(R.id.pooja_desc);
            contentURL = itemView.findViewById(R.id.contentURL);
            rowPlayHint = itemView.findViewById(R.id.rowPlayHint);
            btnDeleteRow = itemView.findViewById(R.id.btnDeleteRow);
            status = itemView.findViewById(R.id.status);
            img = itemView.findViewById(R.id.pooja_img);
            cardview = itemView.findViewById(R.id.mainCard);
        }
    }
}
