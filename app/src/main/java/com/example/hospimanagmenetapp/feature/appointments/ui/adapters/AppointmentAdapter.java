package com.example.hospimanagmenetapp.feature.appointments.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hospimanagmenetapp.R;
import com.example.hospimanagmenetapp.data.entities.Appointment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.VH> {

    public interface Clicker { void onClick(Appointment a); }

    private final List<Appointment> data;
    private final Clicker clicker;
    private final SimpleDateFormat sdf =
            new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.UK);

    public AppointmentAdapter(List<Appointment> data, Clicker clicker) {
        this.data = data;
        this.clicker = clicker;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        Appointment a = data.get(position);
        h.tvPatient.setText("NHS: " + a.patientNhsNumber);
        h.tvClinician.setText(a.clinicianName + " — " + a.clinic);
        h.tvTime.setText(sdf.format(new Date(a.startTime)) + " → " + sdf.format(new Date(a.endTime)));
        h.itemView.setOnClickListener(v -> clicker.onClick(a));
    }

    @Override public int getItemCount() { return data == null ? 0 : data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvPatient, tvClinician, tvTime;
        VH(View item) {
            super(item);
            tvPatient = item.findViewById(R.id.tvPatient);
            tvClinician = item.findViewById(R.id.tvClinician);
            tvTime = item.findViewById(R.id.tvTime);
        }
    }
}
