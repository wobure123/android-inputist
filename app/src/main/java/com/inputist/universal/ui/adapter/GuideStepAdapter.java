package com.inputist.universal.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.inputist.universal.R;
import com.inputist.universal.ui.GuideActivity.GuideStep;

import java.util.ArrayList;
import java.util.List;

/**
 * 使用指南步骤适配器
 */
public class GuideStepAdapter extends RecyclerView.Adapter<GuideStepAdapter.GuideStepViewHolder> {
    
    private List<GuideStep> steps = new ArrayList<>();

    public void setSteps(List<GuideStep> steps) {
        this.steps = steps != null ? steps : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GuideStepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_guide_step, parent, false);
        return new GuideStepViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GuideStepViewHolder holder, int position) {
        GuideStep step = steps.get(position);
        holder.bind(step);
    }

    @Override
    public int getItemCount() {
        return steps.size();
    }

    static class GuideStepViewHolder extends RecyclerView.ViewHolder {
        
        private TextView textEmoji;
        private TextView textStepNumber;
        private TextView textTitle;
        private TextView textDescription;

        public GuideStepViewHolder(@NonNull View itemView) {
            super(itemView);
            
            textEmoji = itemView.findViewById(R.id.text_emoji);
            textStepNumber = itemView.findViewById(R.id.text_step_number);
            textTitle = itemView.findViewById(R.id.text_title);
            textDescription = itemView.findViewById(R.id.text_description);
        }

        public void bind(GuideStep step) {
            textEmoji.setText(step.getEmoji());
            textStepNumber.setText("第" + step.getStepNumber() + "步");
            textTitle.setText(step.getTitle());
            textDescription.setText(step.getDescription());
        }
    }
}
