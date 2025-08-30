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

import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    private Context context;
    private List<String> filePaths;
    private boolean isFavoriteList;
    private OnItemClickListener onItemClickListener;
    
    public interface OnItemClickListener {
        void onItemClick(View view, int position);
        void onItemLongClick(View view, int position);
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public FileAdapter(Context context, List<String> filePaths, boolean isFavoriteList) {
        this.context = context;
        this.filePaths = filePaths;
        this.isFavoriteList = isFavoriteList;
    }

    public void updateData(List<String> newFilePaths) {
        this.filePaths = newFilePaths;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        // 如果是收藏列表且是最后一项，返回添加按钮类型
        if (isFavoriteList && position == filePaths.size()) {
            return 1; // 添加按钮类型
        }
        return 0; // 普通文件项类型
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if (viewType == 1) {
            // 添加按钮项
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_add_favorite, parent, false);
        } else {
            // 普通文件项
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_favorite, parent, false);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        if (getItemViewType(position) == 1) {
            // 添加按钮项
            holder.icon.setImageResource(R.drawable.ic_add);
            holder.name.setText("添加收藏");
        } else {
            // 普通文件项
            String path = filePaths.get(position);
            String name = FileUtils.getFileName(path);
            holder.name.setText(name);
            
            // 设置图标
            Drawable icon;
            if (FileUtils.isDirectory(path)) {
                icon = context.getResources().getDrawable(R.drawable.ic_folder);
            } else {
                // 根据文件类型设置不同图标
                String extension = FileUtils.getFileExtension(path);
                if (extension != null) {
                    switch (extension.toLowerCase()) {
                        case "txt":
                        case "md":
                            icon = context.getResources().getDrawable(R.drawable.ic_text);
                            break;
                        case "jpg":
                        case "jpeg":
                        case "png":
                        case "gif":
                            icon = context.getResources().getDrawable(R.drawable.ic_image);
                            break;
                        case "pdf":
                            icon = context.getResources().getDrawable(R.drawable.ic_pdf);
                            break;
                        default:
                            icon = context.getResources().getDrawable(R.drawable.ic_file);
                    }
                } else {
                    icon = context.getResources().getDrawable(R.drawable.ic_file);
                }
            }
            holder.icon.setImageDrawable(icon);
        }
        
        // 设置点击事件
        if (onItemClickListener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getLayoutPosition();
                    onItemClickListener.onItemClick(holder.itemView, pos);
                }
            });
            
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int pos = holder.getLayoutPosition();
                    onItemClickListener.onItemLongClick(holder.itemView, pos);
                    return true;
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        // 如果是收藏列表，需要额外显示一个"添加收藏"按钮
        return isFavoriteList ? filePaths.size() + 1 : filePaths.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView icon;
        public TextView name;

        public ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.item_icon);
            name = itemView.findViewById(R.id.item_name);
        }
    }
}