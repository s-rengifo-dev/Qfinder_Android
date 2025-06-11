package com.sena.qfinder.ui.home;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

public class MercadoPagoCheckoutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String url = getIntent().getStringExtra("checkout_url");

        if (url == null || url.isEmpty()) {
            finish();
            return;
        }

        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();

        try {
            customTabsIntent.launchUrl(this, Uri.parse(url));
        } catch (Exception e) {
            e.printStackTrace();
        }

        finish();
    }
}