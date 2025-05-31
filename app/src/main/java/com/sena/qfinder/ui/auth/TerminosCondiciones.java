package com.sena.qfinder.ui.auth;

import android.graphics.Color;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;

import com.sena.qfinder.R;

public class TerminosCondiciones extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminos_condiciones, container, false);

        // Configurar WebView
        WebView webView = view.findViewById(R.id.webViewTerminos);
        String htmlContent = buildHtmlContent();

        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
        webView.setBackgroundColor(Color.TRANSPARENT);

        // Configurar botón
        Button btnAceptar = view.findViewById(R.id.btnAceptarTerminos);
        btnAceptar.setOnClickListener(v -> requireActivity().onBackPressed());

        return view;
    }

    private String buildHtmlContent() {
        return "<html>" +
                "<head>" +
                "<style>" +
                "body { color: #000000; text-align: justify; font-family: sans-serif; }" +
                "h1 { text-align: center; font-size: 1.5em; }" +
                "h2 { font-size: 1.2em; margin-top: 20px; }" +
                "p { margin-bottom: 15px; line-height: 1.5; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<h1>" + getString(R.string.terminos_completos) + "</h1>" +
                "<h2>" + getString(R.string.titulo1) + "</h2>" +
                "<p>" + getString(R.string.txtTC1) + "</p>" +
                "<h2>" + getString(R.string.titulo2) + "</h2>" +
                "<p>" + getString(R.string.txtTC2) + "</p>" +
                "<h2>" + getString(R.string.titulo3) + "</h2>" +
                "<p>" + getString(R.string.txtTC3) + "</p>" +
                "<h2>" + getString(R.string.titulo4) + "</h2>" +
                "<p>" + getString(R.string.txtTC4) + "</p>" +
                "<h2>" + getString(R.string.titulo5) + "</h2>" +
                "<p>" + getString(R.string.txtTC5) + "</p>" +
                "<h2>" + getString(R.string.titulo6) + "</h2>" +
                "<p>" + getString(R.string.txtTC6) + "</p>" +
                "<h2>" + getString(R.string.titulo7) + "</h2>" +
                "<p>" + getString(R.string.txtTC7) + "</p>" +
                "<h2>" + getString(R.string.titulo8) + "</h2>" +
                "<p>" + getString(R.string.txtTC8) + "</p>" +
                "<h2>" + getString(R.string.titulo9) + "</h2>" +
                "<p>" + getString(R.string.txtTC9) + "</p>" +
                "<h2>" + getString(R.string.titulo10) + "</h2>" +
                "<p>" + getString(R.string.txtTC10) + "</p>" +
                "</body></html>";
    }
}