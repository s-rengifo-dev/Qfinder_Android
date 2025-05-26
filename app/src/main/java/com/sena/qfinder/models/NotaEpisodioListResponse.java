package com.sena.qfinder.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class NotaEpisodioListResponse {
    @SerializedName("data")
    private List<NotaEpisodio> data;

    public List<NotaEpisodio> getData() {
        return data;
    }

    public void setData(List<NotaEpisodio> data) {
        this.data = data;
    }
}
