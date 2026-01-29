package com.passfamily.airesumebuilder.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.passfamily.airesumebuilder.R;
import com.passfamily.airesumebuilder.model.Resume;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ResumeListAdapter extends RecyclerView.Adapter<ResumeListAdapter.ResumeViewHolder> {

    private Context context;
    private List<Resume> resumes;
    private OnItemClickListener onItemClickListener;
    private OnEditNameClickListener onEditNameClickListener;
    private OnDeleteClickListener onDeleteClickListener;

    public interface OnItemClickListener {
        void onItemClick(Resume resume);
    }

    public interface OnEditNameClickListener {
        void onEditNameClick(Resume resume);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(Resume resume);
    }

    public ResumeListAdapter(Context context) {
        this.context = context;
        this.resumes = new ArrayList<>();
    }

    public void updateResumes(List<Resume> newResumes) {
        this.resumes.clear();
        this.resumes.addAll(newResumes);
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnEditNameClickListener(OnEditNameClickListener listener) {
        this.onEditNameClickListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    @NonNull
    @Override
    public ResumeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_resume, parent, false);
        return new ResumeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResumeViewHolder holder, int position) {
        Resume resume = resumes.get(position);
        holder.bind(resume);
    }

    @Override
    public int getItemCount() {
        return resumes.size();
    }

    class ResumeViewHolder extends RecyclerView.ViewHolder {
        TextView tvResumeName, tvDateTime;
        ImageButton btnEditName, btnDelete;

        public ResumeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvResumeName = itemView.findViewById(R.id.tvResumeName);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            btnEditName = itemView.findViewById(R.id.btnEditName);
            btnDelete = itemView.findViewById(R.id.btnDelete);

            itemView.setOnClickListener(v -> {
                if (onItemClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onItemClickListener.onItemClick(resumes.get(position));
                    }
                }
            });

            btnEditName.setOnClickListener(v -> {
                if (onEditNameClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onEditNameClickListener.onEditNameClick(resumes.get(position));
                    }
                }
            });

            btnDelete.setOnClickListener(v -> {
                if (onDeleteClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onDeleteClickListener.onDeleteClick(resumes.get(position));
                    }
                }
            });
        }

        public void bind(Resume resume) {
            tvResumeName.setText(resume.getResumeName());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
            String dateTime = sdf.format(new Date(resume.getUpdatedAt()));
            tvDateTime.setText(dateTime);
        }
    }
}