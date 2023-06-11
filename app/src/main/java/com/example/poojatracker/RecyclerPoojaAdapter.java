package com.example.poojatracker;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RecyclerPoojaAdapter extends RecyclerView.Adapter<RecyclerPoojaAdapter.ViewHolder> {
    Context context;
    ArrayList<PoojaModel>arrPooja;
    SelectListener listener;
    RecyclerPoojaAdapter(Context context, ArrayList<PoojaModel>arrPooja,SelectListener listener)
    {
        this.context=context;
        this.arrPooja=arrPooja;
        this.listener=listener;

    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.pooja_row,parent,false);
        ViewHolder viewHolder=new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.img.setImageURI(Uri.parse(arrPooja.get(position).img));
        holder.status.setChecked(arrPooja.get(position).status);
        holder.title.setText(arrPooja.get(position).title);
        holder.desc.setText(arrPooja.get(position).desc);
        holder.contentURL.setText(arrPooja.get(position).contentURL);


        //onlick
        holder.cardview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onItemClicked(arrPooja.get(position));
            }
        });

    }

    @Override
    public int getItemCount() {
        return arrPooja.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView title,desc,contentURL;
        CheckBox status;
        ImageView img;
        CardView cardview;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title=itemView.findViewById(R.id.pooja_name);
            desc=itemView.findViewById(R.id.pooja_desc);
            contentURL=itemView.findViewById(R.id.contentURL);
            status=itemView.findViewById(R.id.status);
            img=itemView.findViewById(R.id.pooja_img);
            cardview=itemView.findViewById(R.id.mainCard);


        }
    }
}
