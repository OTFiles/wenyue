package com.otfiles.wenyue.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.otfiles.wenyue.R;
import com.otfiles.wenyue.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DirectoryAdapter extends RecyclerView.Adapter<DirectoryAdapter.ViewHolder> {

    private Context context;
    private List<File> files;
    private List<File> selectedFiles;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(File file);
        void onIconClick(File file);
    }

    public DirectoryAdapter(Context context, List<File> files, OnItemClickListener listener) {
        this.context = context;
        this.files = files;
        this.listener = listener;
        this.selectedFiles = new ArrayList<>();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final File file = files.get(position);
        
        // 设置文件名
        holder.fileName.setText(file.getName());
        
        // 设置图标
        if (file.isDirectory()) {
            holder.itemIcon.setImageResource(R.drawable.ic_folder);
        } else {
            // 根据文件类型设置不同图标
            String extension = FileUtils.getFileExtension(file.getName());
            int iconRes = R.drawable.ic_file; // 默认图标
            
            if (FileUtils.isTextFile(extension)) {
                iconRes = R.drawable.ic_text;
            } else if (FileUtils.isImageFile(extension)) {
                iconRes = R.drawable.ic_image;
            } else if (FileUtils.isAudioFile(extension)) {
                iconRes = R.drawable.ic_audio;
            } else if (FileUtils.isVideoFile(extension)) {
                iconRes = R.drawable.ic_video;
            }
            
            holder.itemIcon.setImageResource(iconRes);
        }
        
        // 设置选中状态
        if (selectedFiles.contains(file)) {
            holder.itemIcon.setColorFilter(context.getResources().getColor(R.color.primary));
        } else {
            holder.itemIcon.clearColorFilter();
        }
        
        // 设置图标点击事件
        holder.itemIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onIconClick(file);
                }
            }
        });
        
        // 设置项点击事件
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onItemClick(file);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return files != null ? files.size() : 0;
    }

    public void updateFiles(List<File> newFiles) {
        this.files = newFiles;
        this.selectedFiles.clear();
        notifyDataSetChanged();
    }

    public void toggleSelection(File file) {
        if (selectedFiles.contains(file)) {
            selectedFiles.remove(file);
        } else {
            selectedFiles.add(file);
        }
        notifyDataSetChanged();
    }

    public void selectAll() {
        selectedFiles.clear();
        selectedFiles.addAll(files);
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedFiles.clear();
        notifyDataSetChanged();
    }

    public List<File> getSelectedFiles() {
        return selectedFiles;
    }

    public boolean isSelectionMode() {
        return !selectedFiles.isEmpty();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView itemIcon;
        TextView fileName;

        public ViewHolder(View itemView) {
            super(itemView);
            itemIcon = itemView.findViewById(R.id.item_icon);
            fileName = itemView.findViewById(R.id.file_name);
        }
    }
}