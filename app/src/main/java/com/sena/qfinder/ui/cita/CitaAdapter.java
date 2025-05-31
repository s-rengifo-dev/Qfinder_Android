package com.sena.qfinder.ui.cita;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sena.qfinder.R;
import com.sena.qfinder.data.models.CitaMedica;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CitaAdapter extends RecyclerView.Adapter<CitaAdapter.CitaViewHolder> {
    private List<CitaMedica> listaCitas;

    public CitaAdapter(List<CitaMedica> listaCitas) {
        this.listaCitas = listaCitas;
    }

    public void updateData(List<CitaMedica> nuevasCitas) {
        this.listaCitas = nuevasCitas != null ? nuevasCitas : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CitaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cita_medica, parent, false);
        return new CitaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CitaViewHolder holder, int position) {
        CitaMedica cita = listaCitas.get(position);
        holder.textFecha.setText(formatFecha(cita.getFechaCita()));
        holder.textMotivo.setText(cita.getMotivoCita());
        holder.textEstado.setText(cita.getEstadoCita());

        // Puedes personalizar más según los nuevos campos
        if (cita.getTitulo() != null && !cita.getTitulo().isEmpty()) {
            holder.textTitulo.setText(cita.getTitulo());
            holder.textTitulo.setVisibility(View.VISIBLE);
        } else {
            holder.textTitulo.setVisibility(View.GONE);
        }
    }

    private String formatFecha(String fecha) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return outputFormat.format(inputFormat.parse(fecha));
        } catch (Exception e) {
            return fecha;
        }
    }

    @Override
    public int getItemCount() {
        return listaCitas.size();
    }

    public static class CitaViewHolder extends RecyclerView.ViewHolder {
        TextView textFecha, textMotivo, textEstado, textTitulo;

        public CitaViewHolder(@NonNull View itemView) {
            super(itemView);
            textFecha = itemView.findViewById(R.id.textFechaCita);
            textMotivo = itemView.findViewById(R.id.textMotivoCita);
            textEstado = itemView.findViewById(R.id.textEstadoCita);
            textTitulo = itemView.findViewById(R.id.textTituloCita);
        }
    }
}