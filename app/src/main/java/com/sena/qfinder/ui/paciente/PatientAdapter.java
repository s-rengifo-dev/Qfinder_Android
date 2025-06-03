package com.sena.qfinder.ui.paciente;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.sena.qfinder.R;
import com.sena.qfinder.data.models.PacienteResponse;

import java.util.List;

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PatientViewHolder> {

    private List<PacienteResponse> patients;
    private OnPatientClickListener listener;

    public interface OnPatientClickListener {
        void onPatientClick(PacienteResponse paciente);
    }

    public PatientAdapter(List<PacienteResponse> patients, OnPatientClickListener listener) {
        this.patients = patients;
        this.listener = listener;
    }

    public void setPatients(List<PacienteResponse> patients) {
        this.patients = patients;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_patient_card, parent, false);
        return new PatientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
        PacienteResponse patient = patients.get(position);
        holder.bind(patient);
        holder.itemView.setOnClickListener(v -> listener.onPatientClick(patient));
    }

    @Override
    public int getItemCount() {
        return patients != null ? patients.size() : 0;
    }

    static class PatientViewHolder extends RecyclerView.ViewHolder {
        private TextView tvPatientName;
        private TextView tvPatientConditions;

        private ImageView ivProfile;

        public PatientViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatientName = itemView.findViewById(R.id.tvPatientName);
            tvPatientConditions = itemView.findViewById(R.id.tvPatientConditions);
            ivProfile = itemView.findViewById(R.id.ivPatientProfile);
        }

        public void bind(PacienteResponse patient) {
            tvPatientName.setText(patient.getNombre() + " " + patient.getApellido());
            tvPatientConditions.setText(patient.getDiagnostico_principal() != null ? patient.getDiagnostico_principal() : "Sin diagnóstico");


            String imagenUrl = patient.getImagen_paciente();
            if (imagenUrl != null && !imagenUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(imagenUrl)
                        .placeholder(R.drawable.perfil_familiar)
                        .error(R.drawable.perfil_familiar)
                        .circleCrop()
                        .into(ivProfile);
            } else {
                ivProfile.setImageResource(R.drawable.perfil_familiar);
            }
        }
    }
}

