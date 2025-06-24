package com.inputist.universal.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.inputist.universal.R;
import com.inputist.universal.model.Action;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Action列表适配器
 */
public class ActionAdapter extends RecyclerView.Adapter<ActionAdapter.ActionViewHolder> {
    
    private List<Action> actions = new ArrayList<>();
    private OnActionClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public ActionAdapter(OnActionClickListener listener) {
        this.listener = listener;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions != null ? actions : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ActionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_action, parent, false);
        return new ActionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActionViewHolder holder, int position) {
        Action action = actions.get(position);
        holder.bind(action);
    }

    @Override
    public int getItemCount() {
        return actions.size();
    }

    class ActionViewHolder extends RecyclerView.ViewHolder {
        
        private TextView textName;
        private TextView textSystemPrompt;
        private TextView textModifiedTime;
        private ImageButton buttonEdit;
        private ImageButton buttonDelete;

        public ActionViewHolder(@NonNull View itemView) {
            super(itemView);
            
            textName = itemView.findViewById(R.id.text_action_name);
            textSystemPrompt = itemView.findViewById(R.id.text_system_prompt);
            textModifiedTime = itemView.findViewById(R.id.text_modified_time);
            buttonEdit = itemView.findViewById(R.id.button_edit);
            buttonDelete = itemView.findViewById(R.id.button_delete);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onActionClick(actions.get(position));
                }
            });
            
            buttonEdit.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onActionEdit(actions.get(position));
                }
            });
            
            buttonDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onActionDelete(actions.get(position));
                }
            });
        }

        public void bind(Action action) {
            textName.setText(action.getName());
            
            // 显示系统指令的预览（最多2行）
            String preview = action.getSystemPrompt();
            if (preview.length() > 100) {
                preview = preview.substring(0, 100) + "...";
            }
            textSystemPrompt.setText(preview);
            
            // 显示修改时间
            String timeText = "修改时间: " + dateFormat.format(new Date(action.getModifiedTime()));
            textModifiedTime.setText(timeText);
        }
    }

    public interface OnActionClickListener {
        void onActionClick(Action action);
        void onActionEdit(Action action);
        void onActionDelete(Action action);
    }
}
