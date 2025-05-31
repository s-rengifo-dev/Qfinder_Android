package com.sena.qfinder;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.sena.qfinder.api.AuthService;
import com.sena.qfinder.models.PacienteListResponse;
import com.sena.qfinder.models.PacienteResponse;
import com.sena.qfinder.ui.home.EditarPacienteDialogFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PerfilPaciente extends Fragment implements EditarPacienteDialogFragment.OnPacienteActualizadoListener {

    // Constantes
    private static final String ARG_PACIENTE_ID = "paciente_id";
    private static final String BASE_URL = "https://qfinder-production.up.railway.app/";
    private static final int QR_CODE_SIZE = 800;
    private static final String KEY_PACIENTE = "saved_paciente";
    private static final String KEY_IMAGE_DATA = "saved_image_data";

    // Variables
    private int pacienteId;
    private SharedPreferences sharedPreferences;
    private AuthService authService;
    private PacienteResponse pacienteActual;
    private String currentImageData;

    // Views
    private TextView tvNombreApellido, tvFechaNacimiento, tvSexo, tvDiagnostico, tvIdentificacion;
    private ImageView btnBack, ivCodigoQR, imagenPerfilP;
    private ProgressBar progressBar;

    public static PerfilPaciente newInstance(int pacienteId) {
        PerfilPaciente fragment = new PerfilPaciente();
        Bundle args = new Bundle();
        args.putInt(ARG_PACIENTE_ID, pacienteId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            pacienteId = getArguments().getInt(ARG_PACIENTE_ID);
        }

        sharedPreferences = requireActivity().getSharedPreferences("usuario", Context.MODE_PRIVATE);

        // Restaurar estado si existe
        if (savedInstanceState != null) {
            pacienteActual = savedInstanceState.getParcelable(KEY_PACIENTE);
            currentImageData = savedInstanceState.getString(KEY_IMAGE_DATA);
            Log.d("PerfilPaciente", "Restaurando estado - Imagen: " + (currentImageData != null ? currentImageData.substring(0, Math.min(20, currentImageData.length())) : "null"));
        }

        initializeRetrofit();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_PACIENTE, pacienteActual);
        outState.putString(KEY_IMAGE_DATA, currentImageData);
        Log.d("PerfilPaciente", "Guardando estado - Imagen: " + (currentImageData != null ? currentImageData.substring(0, Math.min(20, currentImageData.length())) : "null"));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil_paciente, container, false);
        initViews(view);
        setupClickListeners(view);

        if (pacienteActual != null) {
            displayPacienteData(pacienteActual);
            mostrarQRDelPaciente(pacienteActual);
        } else {
            loadPacienteData();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recargar datos al volver al fragment
        if (pacienteActual != null) {
            Log.d("PerfilPaciente", "OnResume - Recargando datos");
            loadPacienteData();
        }
    }

    private void initViews(View view) {
        tvNombreApellido = view.findViewById(R.id.tvNombreApellido);
        tvFechaNacimiento = view.findViewById(R.id.tvFechaNacimiento);
        tvSexo = view.findViewById(R.id.tvSexo);
        tvDiagnostico = view.findViewById(R.id.tvDiagnostico);
        tvIdentificacion = view.findViewById(R.id.tvIdentificacion);
        btnBack = view.findViewById(R.id.btnBack);
        ivCodigoQR = view.findViewById(R.id.imgQrPaciente);
        imagenPerfilP = view.findViewById(R.id.ivFotoPerfil);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupClickListeners(View view) {
        btnBack.setOnClickListener(v -> requireActivity().onBackPressed());
        view.findViewById(R.id.boton_imagen).setOnClickListener(v -> abrirDialogoEditar());
    }

    private void initializeRetrofit() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        authService = retrofit.create(AuthService.class);
    }

    private void loadPacienteData() {
        String token = sharedPreferences.getString("token", null);
        if (token == null) {
            showError("Sesión no válida");
            return;
        }

        showLoading(true);
        authService.listarPacientes("Bearer " + token).enqueue(new Callback<PacienteListResponse>() {
            @Override
            public void onResponse(@NonNull Call<PacienteListResponse> call, @NonNull Response<PacienteListResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    for (PacienteResponse paciente : response.body().getData()) {
                        if (paciente.getId() == pacienteId) {
                            pacienteActual = paciente;
                            currentImageData = paciente.getImagen_paciente();
                            displayPacienteData(paciente);
                            mostrarQRDelPaciente(paciente);
                            Log.d("PerfilPaciente", "Datos cargados - Imagen: " + (currentImageData != null ? currentImageData.substring(0, Math.min(20, currentImageData.length())) : "null"));
                            return;
                        }
                    }
                    showError("Paciente no encontrado");
                } else {
                    showError("Error al cargar datos del paciente");
                    try {
                        Log.e("API_ERROR", "Error: " + response.errorBody().string());
                    } catch (Exception e) {
                        Log.e("API_ERROR", "Error al leer errorBody", e);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<PacienteListResponse> call, @NonNull Throwable t) {
                showLoading(false);
                showError("Error de conexión: " + t.getMessage());
                Log.e("API_FAILURE", "Error en la llamada API", t);
            }
        });
    }

    private void displayPacienteData(PacienteResponse paciente) {
        if (!isAdded()) return;

        tvNombreApellido.setText(paciente.getNombre() + " " + paciente.getApellido());
        tvFechaNacimiento.setText("FECHA DE NACIMIENTO: " + formatFecha(paciente.getFecha_nacimiento()));
        tvSexo.setText("SEXO: " + capitalizeFirstLetter(paciente.getSexo()));
        tvDiagnostico.setText("DIAGNÓSTICO: " + safeText(paciente.getDiagnostico_principal()));
        tvIdentificacion.setText("IDENTIFICACIÓN: " + safeText(paciente.getIdentificacion()));

        loadProfileImage(paciente.getImagen_paciente());
    }

    private void loadProfileImage(String imageData) {
        if (!isAdded() || imagenPerfilP == null || imageData == null) return;

        Log.d("PerfilPaciente", "Cargando imagen. Datos: " + imageData.substring(0, Math.min(20, imageData.length())) + "...");

        // Limpiar completamente la imagen antes de cargar
        imagenPerfilP.setImageDrawable(null);

        try {
            if (imageData.startsWith("http")) {
                // Forzar recarga con timestamp único
                String urlWithTimestamp = imageData + "?t=" + System.currentTimeMillis();
                Log.d("PerfilPaciente", "Cargando imagen desde URL: " + urlWithTimestamp);

                Glide.with(requireContext())
                        .load(urlWithTimestamp)
                        .apply(new RequestOptions()
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .skipMemoryCache(true)
                                .circleCrop())
                        .into(imagenPerfilP);
            } else {
                // Manejo más robusto de Base64
                String base64Image = imageData.contains(",") ?
                        imageData.substring(imageData.indexOf(",") + 1) : imageData;

                byte[] imageBytes = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                // Usar Glide también para imágenes Base64 para consistencia
                Glide.with(requireContext())
                        .load(bitmap)
                        .apply(new RequestOptions()
                                .circleCrop())
                        .into(imagenPerfilP);
            }
            currentImageData = imageData;
        } catch (Exception e) {
            Log.e("FOTO_PERFIL", "Error al cargar foto: " + e.getMessage(), e);
            imagenPerfilP.setImageResource(R.drawable.perfil_paciente); // Imagen por defecto
        }
    }



    private void mostrarQRDelPaciente(PacienteResponse paciente) {
        if (paciente.getQrCode() != null && !paciente.getQrCode().isEmpty()) {
            mostrarCodigoQR(paciente.getQrCode());
        } else {
            generarQRLocalUniversal(paciente);
        }
    }

    private void mostrarCodigoQR(String base64QR) {
        try {
            String pureBase64 = base64QR.contains(",") ? base64QR.substring(base64QR.indexOf(",") + 1) : base64QR;
            byte[] decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            ivCodigoQR.setImageBitmap(bitmap);
            ivCodigoQR.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e("QR_ERROR", "Error al mostrar QR", e);
            generarQRLocalUniversal(pacienteActual);
        }
    }

    private void generarQRLocalUniversal(PacienteResponse paciente) {
        try {
            String qrContent = crearContenidoQRUniversal(paciente);
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);

            Bitmap bitmap = Bitmap.createBitmap(QR_CODE_SIZE, QR_CODE_SIZE, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < QR_CODE_SIZE; x++) {
                for (int y = 0; y < QR_CODE_SIZE; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            ivCodigoQR.setImageBitmap(bitmap);
            ivCodigoQR.setVisibility(View.VISIBLE);
        } catch (WriterException e) {
            Log.e("QR_GEN", "Error generando QR", e);
            showError("No se pudo generar el código QR");
        }
    }

    private String crearContenidoQRUniversal(PacienteResponse paciente) {
        return "ID: " + paciente.getId() + "\n" +
                "Nombre: " + paciente.getNombre() + " " + paciente.getApellido() + "\n" +
                "Identificación: " + safeText(paciente.getIdentificacion()) + "\n" +
                "Fecha Nacimiento: " + formatFecha(paciente.getFecha_nacimiento()) + "\n" +
                "Sexo: " + capitalizeFirstLetter(paciente.getSexo()) + "\n" +
                "Diagnóstico: " + safeText(paciente.getDiagnostico_principal()) + "\n" +
                "App: QfindeR";
    }

    private void abrirDialogoEditar() {
        if (pacienteActual == null) {
            showToast("No hay paciente cargado para editar");
            return;
        }
        EditarPacienteDialogFragment dialog = new EditarPacienteDialogFragment(pacienteActual);
        dialog.setOnPacienteActualizadoListener(this);
        dialog.show(getParentFragmentManager(), "editarPaciente");
    }
    @Override
    public void onPacienteActualizado(PacienteResponse pacienteActualizado) {
        Log.d("PerfilPaciente", "Paciente actualizado recibido - ID: " + pacienteActualizado.getId());

        // Forzar recarga completa de datos
        pacienteActual = pacienteActualizado;
        currentImageData = pacienteActualizado.getImagen_paciente();

        // Limpiar Glide completamente
        new Handler(Looper.getMainLooper()).post(() -> {
            Glide.with(requireContext()).clear(imagenPerfilP);
            displayPacienteData(pacienteActualizado);
        });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        Log.e("PerfilPaciente", message);
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private String formatFecha(String fechaOriginal) {
        try {
            SimpleDateFormat original = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat formatoDeseado = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date fecha = original.parse(fechaOriginal);
            return formatoDeseado.format(fecha);
        } catch (Exception e) {
            return fechaOriginal != null ? fechaOriginal : "N/A";
        }
    }

    private String capitalizeFirstLetter(String str) {
        return (str != null && !str.isEmpty()) ? str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase() : "No especificado";
    }

    private String safeText(String text) {
        return text != null && !text.trim().isEmpty() ? text : "No especificado";
    }
}