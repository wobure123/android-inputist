package com.inputassistant.universal.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.inputassistant.universal.R;
import com.inputassistant.universal.model.Action;

import java.util.ArrayList;
import java.util.List;

/**
 * Action列表适配器
 */
public class ActionAdapter extends RecyclerView.Adapter<ActionAdapter.ActionViewHolder> {
    private List<Action> actions = new ArrayList<>();
    private OnActionClickListener listener;

    public interface OnActionClickListener {
        void onActionEdit(Action action);
        void onActionDelete(Action action);
    }

    public ActionAdapter(OnActionClickListener listener) {
        this.listener = listener;
    }

    public void updateActions(List<Action> newActions) {
        this.actions.clear();
        this.actions.addAll(newActions);
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
        private TextView tvName;
        private TextView tvSystemPrompt;
        private ImageButton btnEdit;
        private ImageButton btnDelete;

        public ActionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_action_name);
            tvSystemPrompt = itemView.findViewById(R.id.tv_action_system_prompt);
            btnEdit = itemView.findViewById(R.id.btn_edit_action);
            btnDelete = itemView.findViewById(R.id.btn_delete_action);
        }

        public void bind(Action action) {
            tvName.setText(action.getName());
            
            // 显示系统指令的前100个字符
            String systemPrompt = action.getSystemPrompt();
            if (systemPrompt.length() > 100) {
                systemPrompt = systemPrompt.substring(0, 97) + "...";
            }
            tvSystemPrompt.setText(systemPrompt);

            btnEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onActionEdit(action);
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onActionDelete(action);
                }
            });
        }
    }
}
