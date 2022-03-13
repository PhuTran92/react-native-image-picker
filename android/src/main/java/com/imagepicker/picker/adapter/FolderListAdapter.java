package com.imagepicker.picker.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.imagepicker.R;
import com.imagepicker.picker.model.Folder;
import com.imagepicker.picker.model.Image;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yuyh.
 * @date 2016/8/5.
 */
public class FolderListAdapter extends RecyclerView.Adapter<FolderListAdapter.ViewHolder> {

    public interface Listener {
        void onFolderClick(int position, Folder item);
    }

    private List<Folder> currentData;
    private Folder selectedFolder;

    public FolderListAdapter.Listener listener;

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView;
        TextView tvName;
        TextView tvNumOfImages;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivImage);
            tvName = itemView.findViewById(R.id.tvName);
            tvNumOfImages = itemView.findViewById(R.id.tvNumOfImages);
        }
    }

    public void submitData(List<Folder> data, Folder folder) {
        currentData = new ArrayList<>(data);
        selectedFolder = folder;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FolderListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderListAdapter.ViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        Folder folder = currentData.get(position);
        Glide.with(holder.itemView.getContext()).load(folder.cover.path).transform(new CenterCrop(), new RoundedCorners(25))
                .into(holder.imageView);

        holder.tvName.setText(folder.name);
        holder.tvNumOfImages.setText(String.valueOf(folder.images.size()));

        boolean isSelected = selectedFolder.name.equals(folder.name);
        if (isSelected) {
            holder.itemView.setBackgroundResource(R.color.grey);
        } else {
            holder.itemView.setBackgroundResource(0);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onFolderClick(position, currentData.get(position));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        if (currentData == null) {
            return 0;
        }
        return currentData.size();
    }
}
