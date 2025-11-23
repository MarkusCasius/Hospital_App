package com.example.hospimanagmenetapp.ui.adapters; // Adapter classes for RecyclerView live here

import android.view.LayoutInflater; // Inflates XML layouts into View objects
import android.view.View;           // Base class for all UI components
import android.view.ViewGroup;      // Container that holds child views
import android.widget.TextView;     // UI widget for displaying text

import androidx.annotation.NonNull;         // Hint for null-safety contracts
import androidx.recyclerview.widget.RecyclerView; // Support library RecyclerView

import com.example.hospimanagmenetapp.R;              // Resource IDs (layouts, strings, etc.)
import com.example.hospimanagmenetapp.data.entities.Staff; // Model class rendered by this adapter

import java.util.List; // List interface for holding items

public class StaffAdapter extends RecyclerView.Adapter<StaffAdapter.VH> { // Adapter bridges Staff data to RecyclerView rows

    private final List<Staff> data; // Backing data set rendered by the list
    private final OnStaffClickListener listener; // Listener for click events

    public interface OnStaffClickListener {
        void onStaffClick(Staff staff);
    }

    public StaffAdapter(List<Staff> data, OnStaffClickListener listener) { // Constructor takes the data to display
        this.data = data;                   // Store the list for later binding
        this.listener = listener;           // Store the listener
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate a single row (item_staff.xml) and wrap it in a ViewHolder
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_staff, parent, false); // false = don’t attach yet; RecyclerView will handle it
        return new VH(v); // Return a new ViewHolder instance bound to this row view
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        // Bind the Staff item at 'position' to the views in the holder
        Staff s = data.get(position);     // Fetch the model for this row
        h.bind(s, listener);
    }

    @Override
    public int getItemCount() {
        // RecyclerView asks how many rows to display
        return data == null ? 0 : data.size(); // Null-safe size (0 if data not set)
    }

    // ViewHolder caches view references for each row to avoid repeated findViewById calls
    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvRole; // Row widgets for name, email, and role

        VH(@NonNull View itemView) { // Constructed with the inflated row view
            super(itemView);                                  // Pass to base class
            tvName = itemView.findViewById(R.id.tvStaffName); // Bind TextView for name
            tvEmail = itemView.findViewById(R.id.tvStaffEmail); // Bind TextView for email
            tvRole = itemView.findViewById(R.id.tvStaffRole); // Bind TextView for role
        }

        void bind(final Staff staff, final OnStaffClickListener listener) {
            tvName.setText(staff.fullName);     // Show staff member’s name
            tvEmail.setText(staff.email);       // Show staff member’s email
            tvRole.setText(staff.role.name());  // Show role (enum name as text)

            // Set the listener on the entire row view
            itemView.setOnClickListener(v -> listener.onStaffClick(staff));
        }
    }
}