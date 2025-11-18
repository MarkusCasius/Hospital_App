package com.example.hospimanagmenetapp.feature.ehr.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.data.entities.Vitals;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VitalsAdapter extends ListAdapter<Vitals, VitalsAdapter.VitalsViewHolder> {

    public VitalsAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public VitalsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.vitals_list_item, parent, false);
        return new VitalsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VitalsViewHolder holder, int position) {
        Vitals vitals = getItem(position);
        if (vitals != null) {
            holder.bind(vitals);
        }
    }

    // ViewHolder class for displaying a single item
    static class VitalsViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTimestamp;
        private final TextView tvVitalsDetails;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());

        VitalsViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvVitalsDetails = itemView.findViewById(R.id.tvVitalsDetails);
        }

        void bind(Vitals vitals) {
            tvTimestamp.setText(dateFormat.format(new Date(vitals.timestamp)));
            String details = String.format(Locale.UK,
                    "BP: %d/%d mmHg | HR: %d bpm | Temp: %.1f°C",
                    vitals.systolic,
                    vitals.diastolic,
                    vitals.heartRate,
                    vitals.temperature);
            tvVitalsDetails.setText(details);
        }
    }

    // DiffUtil.ItemCallback tells the adapter how to compute list updates efficiently.
    private static final DiffUtil.ItemCallback<Vitals> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Vitals>() {
                @Override
                public boolean areItemsTheSame(@NonNull Vitals oldItem, @NonNull Vitals newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull Vitals oldItem, @NonNull Vitals newItem) {
                    return oldItem.id == newItem.id &&
                            oldItem.timestamp == newItem.timestamp &&
                            oldItem.systolic == newItem.systolic;
                }
            };
}
