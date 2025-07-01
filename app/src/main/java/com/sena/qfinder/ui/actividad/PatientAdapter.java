package com.sena.qfinder.ui.actividad;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.sena.qfinder.R;
import com.sena.qfinder.data.models.PacienteResponse;

import java.util.ArrayList;
import java.util.List;

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PatientViewHolder> {

    private List<PacienteResponse> patients;
    private final OnPatientClickListener listener;
    private int selectedPatientId = -1;
    private Context context;

    public interface OnPatientClickListener {
        void onPatientClick(PacienteResponse paciente);
    }

    public PatientAdapter(List<PacienteResponse> patients, OnPatientClickListener listener) {
        this.patients = patients;
        this.listener = listener;
    }

    public void setPatients(List<PacienteResponse> patients) {
        this.patients = patients != null ? patients : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setSelectedPatientId(int patientId) {
        this.selectedPatientId = patientId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_patient_card, parent, false);
        return new PatientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
        PacienteResponse patient = patients.get(position);
        holder.bind(patient);

        // Actualizar apariencia según selección
        updateCardAppearance(holder.itemView, patient.getId() == selectedPatientId);

        holder.itemView.setOnClickListener(v -> listener.onPatientClick(patient));
    }

    private void updateCardAppearance(View cardView, boolean isSelected) {
        GradientDrawable drawable = (GradientDrawable) ContextCompat.getDrawable(context, R.drawable.card_background).mutate();
        cardView.setBackground(drawable);

        if (isSelected) {
            drawable.setStroke(4, ContextCompat.getColor(context, R.color.selected_stroke_color));
            drawable.setColor(ContextCompat.getColor(context, R.color.selected_card_color));
            cardView.setElevation(8f);
        } else {
            drawable.setStroke(1, ContextCompat.getColor(context, R.color.default_stroke_color));
            drawable.setColor(ContextCompat.getColor(context, R.color.default_card_color));
            cardView.setElevation(2f);
        }
    }

    @Override
    public int getItemCount() {
        return patients.size();
    }

    static class PatientViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvConditions;
        private final ImageView ivProfile;

        public PatientViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPatientName);
            tvConditions = itemView.findViewById(R.id.tvPatientConditions);
            ivProfile = itemView.findViewById(R.id.ivPatientProfile);
        }

        public void bind(PacienteResponse patient) {
            tvName.setText(patient.getNombre() + " " + patient.getApellido());

            String diagnostico = patient.getDiagnostico_principal() != null ?
                    patient.getDiagnostico_principal() : "Sin diagnóstico";
            tvConditions.setText(diagnostico);

            if (patient.getImagen_paciente() != null && !patient.getImagen_paciente().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(patient.getImagen_paciente())
                        .placeholder(R.drawable.perfil_paciente)
                        .error(R.drawable.perfil_paciente)
                        .circleCrop()
                        .into(ivProfile);
            } else {
                ivProfile.setImageResource(R.drawable.perfil_paciente);
            }
        }
    }
}