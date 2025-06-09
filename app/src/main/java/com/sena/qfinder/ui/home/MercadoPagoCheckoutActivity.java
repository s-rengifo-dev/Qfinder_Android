package com.sena.qfinder.ui.home;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

public class MercadoPagoCheckoutActivity extends AppCompatActivity {

    private String planType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String checkoutUrl = getIntent().getStringExtra("checkout_url");
        planType = getIntent().getStringExtra("plan_type");

        if (checkoutUrl == null || checkoutUrl.isEmpty()) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        // Guardar el plan en preferencia temporal o clase Singleton si necesitas accederlo luego
        launchChromeCustomTab(checkoutUrl);
    }

    private void launchChromeCustomTab(String url) {
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setShowTitle(true); // Mostrar título de la página
        builder.setInstantAppsEnabled(true); // Permitir Instant Apps si están disponibles

        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(this, Uri.parse(url));

        // Finalizamos esta actividad para que el usuario no pueda volver a ella desde el botón "Atrás"
        finish();
    }
}
