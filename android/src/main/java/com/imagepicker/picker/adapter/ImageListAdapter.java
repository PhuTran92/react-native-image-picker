package com.imagepicker.picker.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.imagepicker.R;
import com.imagepicker.picker.model.Image;

import java.util.ArrayList;
import java.util.List;

public class ImageListAdapter extends RecyclerView.Adapter<ImageListAdapter.ViewHolder> {

    public interface Listener {
        void onImageClick(int position, Image item);
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView;
        ImageView imgCheck;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivImage);
            imgCheck = itemView.findViewById(R.id.ivPhotoChecked);
        }
    }

    public Listener listener;

    private List<Image> currentData;
    private List<String> selectedImages;

    public void setData(List<Image> data, List<String> selected) {
        currentData = new ArrayList<>(data);
        selectedImages = new ArrayList<>(selected);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onImageClick(position, currentData.get(position));
                }
            }
        });

        Glide.with(holder.itemView.getContext()).load(currentData.get(position).path).into(holder.imageView);

        if (selectedImages.contains(currentData.get(position).path)) {
            holder.imgCheck.setImageResource(R.drawable.vector_ic_checked);
        } else {
            holder.imgCheck.setImageResource(0);
        }
    }

    @Override
    public int getItemCount() {
        if (currentData == null) {
            return 0;
        }
        return currentData.size();
    }
}
