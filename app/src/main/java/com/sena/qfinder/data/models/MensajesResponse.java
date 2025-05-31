package com.sena.qfinder.data.models;

import java.util.ArrayList;
import java.util.List;

public class MensajesResponse {
    private List<Mensaje> messages = new ArrayList<>(); // Inicializada por defecto

    public List<Mensaje> getMessages() {
        return messages;
    }

    public void setMessages(List<Mensaje> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
    }
}