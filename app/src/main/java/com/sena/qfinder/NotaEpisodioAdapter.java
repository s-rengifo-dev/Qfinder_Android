package com.sena.qfinder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.sena.qfinder.models.NotaEpisodio;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotaEpisodioAdapter extends BaseAdapter {

    private Context context;
    private List<NotaEpisodio> notas;
    private LayoutInflater inflater;

    // Formato esperado (se puede ajustar según API, aquí sin zona horaria)
    private SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
    private SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public NotaEpisodioAdapter(Context context, List<NotaEpisodio> notas) {
        this.context = context;
        this.notas = notas;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return notas.size();
    }

    @Override
    public Object getItem(int position) {
        return notas.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position; // no id único disponible
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_nota, parent, false);
            holder = new ViewHolder();
            holder.tvTitulo = convertView.findViewById(R.id.tvTituloNota);
            holder.tvDescripcion = convertView.findViewById(R.id.textDescripcion);
            holder.tvSeveridad = convertView.findViewById(R.id.textSeveridad);
            holder.tvFecha = convertView.findViewById(R.id.textFecha);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        NotaEpisodio nota = notas.get(position);

        // Título: resumen de la descripción (hasta 30 chars)
        String titulo = nota.getDescripcion() != null ? nota.getDescripcion() : "";
        if (titulo.length() > 30) {
            titulo = titulo.substring(0, 30) + "...";
        }
        holder.tvTitulo.setText(titulo);

        // Descripción: intervenciones o fallback
        holder.tvDescripcion.setText(nota.getIntervenciones() != null ? nota.getIntervenciones() : "Sin intervenciones");

        // Severidad (texto)
        holder.tvSeveridad.setText("Severidad: " + (nota.getSeveridad() != null ? nota.getSeveridad() : "N/A"));

        // Fecha formateada
        String fechaFormateada = nota.getFechaHoraInicio() != null ? nota.getFechaHoraInicio() : "";
        try {
            Date fecha = inputFormat.parse(nota.getFechaHoraInicio());
            fechaFormateada = outputFormat.format(fecha);
        } catch (ParseException | NullPointerException e) {
            // Si hay error, dejamos la fecha tal cual viene
        }
        holder.tvFecha.setText("Fecha: " + fechaFormateada);

        return convertView;
    }

    private static class ViewHolder {
        TextView tvTitulo;
        TextView tvDescripcion;
        TextView tvSeveridad;
        TextView tvFecha;
    }
}
