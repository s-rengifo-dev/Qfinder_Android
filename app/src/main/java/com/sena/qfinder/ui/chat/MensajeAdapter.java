package com.sena.qfinder.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sena.qfinder.R;
import com.sena.qfinder.data.models.Mensaje;

import java.util.List;

public class MensajeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<Mensaje> mensajes;
    private final String currentUserId;

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;

    public MensajeAdapter(List<Mensaje> mensajes, String currentUserId) {
        this.mensajes = mensajes;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        Mensaje mensaje = mensajes.get(position);
        return mensaje.getIdUsuario().equals(currentUserId) ?
                VIEW_TYPE_MESSAGE_SENT : VIEW_TYPE_MESSAGE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_mensaje_propio, parent, false);
            return new SentMessageHolder(view);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_mensaje_recibido, parent, false);
            return new ReceivedMessageHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Mensaje mensaje = mensajes.get(position);

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_SENT:
                ((SentMessageHolder) holder).bind(mensaje);
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((ReceivedMessageHolder) holder).bind(mensaje);
        }
    }

    @Override
    public int getItemCount() {
        return mensajes.size();
    }

    // Método para actualizar los mensajes
    public void agregarMensajesUnicos(List<Mensaje> nuevosMensajes) {
        for (Mensaje nuevo : nuevosMensajes) {
            boolean yaExiste = false;
            for (Mensaje existente : mensajes) {
                if (existente.getId().equals(nuevo.getId())) {
                    yaExiste = true;
                    break;
                }
            }
            if (!yaExiste) {
                mensajes.add(nuevo);
                notifyItemInserted(mensajes.size() - 1);
            }
        }
    }


    // ViewHolder para mensajes enviados
    private static class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView txtNombreUsuario, txtContenidoMensaje, txtHoraMensaje, txtEstado;

        SentMessageHolder(View itemView) {
            super(itemView);
            txtNombreUsuario = itemView.findViewById(R.id.txtNombreUsuario);
            txtContenidoMensaje = itemView.findViewById(R.id.txtContenidoMensaje);
            txtHoraMensaje = itemView.findViewById(R.id.txtHoraMensaje);
            txtEstado = itemView.findViewById(R.id.txtEstadoMensaje);
        }

        void bind(Mensaje mensaje) {
            txtNombreUsuario.setText("Tú");
            txtContenidoMensaje.setText(mensaje.getContenido());
            txtHoraMensaje.setText(mensaje.getHora());

            if ("pendiente".equals(mensaje.getEstado())) {
                txtEstado.setText("(Enviando...)");
                txtEstado.setVisibility(View.VISIBLE);
            } else if ("error".equals(mensaje.getEstado())) {
                txtEstado.setText("(Error al enviar)");
                txtEstado.setVisibility(View.VISIBLE);
            } else {
                txtEstado.setVisibility(View.GONE);
            }
        }
    }

    // ViewHolder para mensajes recibidos
    private static class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView txtNombreUsuario, txtContenidoMensaje, txtHoraMensaje;

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            txtNombreUsuario = itemView.findViewById(R.id.txtNombreUsuario);
            txtContenidoMensaje = itemView.findViewById(R.id.txtContenidoMensaje);
            txtHoraMensaje = itemView.findViewById(R.id.txtHoraMensaje);
        }

        void bind(Mensaje mensaje) {
            txtNombreUsuario.setText(mensaje.getNombreUsuario());
            txtContenidoMensaje.setText(mensaje.getContenido());
            txtHoraMensaje.setText(mensaje.getHora());
        }
    }
}