package com.sena.qfinder.ui.auth;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.sena.qfinder.R;

public class Registro_usuario extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro_usuario);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.contenedor_fragmentos, new RegistroUsuario(), "TAG_REGISTRO")
                    .commit();
        }
    }
}