package com.sena.qfinder;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
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
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.sena.qfinder.api.AuthService;
import com.sena.qfinder.models.PacienteListResponse;
import com.sena.qfinder.models.PacienteResponse;
import com.sena.qfinder.ui.home.DashboardFragment;
import com.sena.qfinder.ui.home.EditarPacienteDialogFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PerfilPaciente extends Fragment implements EditarPacienteDialogFragment.OnPacienteActualizadoListener {

    private static final String ARG_PACIENTE_ID = "paciente_id";
    private static final String BASE_URL = "https://qfinder-production.up.railway.app/";
    private static final int QR_CODE_SIZE = 800;

    private int pacienteId;
    private SharedPreferences sharedPreferences;
    private AuthService authService;

    private TextView tvNombreApellido, tvFechaNacimiento, tvSexo, tvDiagnostico, tvIdentificacion;
    private ImageView btnBack, ivCodigoQR;
    private ProgressBar progressBar;

    private PacienteResponse pacienteActual;

    public PerfilPaciente() {}

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
        initializeRetrofit();
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil_paciente, container, false);

        ImageView btnEditar = view.findViewById(R.id.boton_imagen);
        ivCodigoQR = view.findViewById(R.id.imgQrPaciente);
        btnEditar.setOnClickListener(v -> abrirDialogoEditar());

        initViews(view);
        setupBackButton();
        loadPacienteData();

        return view;
    }

    private void initViews(View view) {
        tvNombreApellido = view.findViewById(R.id.tvNombreApellido);
        tvFechaNacimiento = view.findViewById(R.id.tvFechaNacimiento);
        tvSexo = view.findViewById(R.id.tvSexo);
        tvDiagnostico = view.findViewById(R.id.tvDiagnostico);
        tvIdentificacion = view.findViewById(R.id.tvIdentificacion);
        btnBack = view.findViewById(R.id.btnBack);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupBackButton() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> navigateBack());
        }
    }

    private void loadPacienteData() {
        String token = sharedPreferences.getString("token", null);
        if (token == null) {
            showError("Sesión no válida");
            return;
        }

        showLoading(true);

        Call<PacienteListResponse> call = authService.listarPacientes("Bearer " + token);

        call.enqueue(new Callback<PacienteListResponse>() {
            @Override
            public void onResponse(Call<PacienteListResponse> call, Response<PacienteListResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<PacienteResponse> pacientes = response.body().getData();
                    for (PacienteResponse paciente : pacientes) {
                        if (paciente.getId() == pacienteId) {
                            pacienteActual = paciente;
                            displayPacienteData(paciente);
                            mostrarQRDelPaciente(paciente);
                            return;
                        }
                    }
                    showError("Paciente no encontrado");
                } else {
                    showError("Error al cargar datos del paciente");
                }
            }

            @Override
            public void onFailure(Call<PacienteListResponse> call, Throwable t) {
                showLoading(false);
                showError("Error de conexión: " + t.getMessage());
            }
        });
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
            String pureBase64 = base64QR.contains(",") ?
                    base64QR.substring(base64QR.indexOf(",") + 1) :
                    base64QR;

            byte[] decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

            ivCodigoQR.setImageBitmap(bitmap);
            ivCodigoQR.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            Log.e("QR_ERROR", "Error al mostrar QR", e);
            if (pacienteActual != null) {
                generarQRLocalUniversal(pacienteActual);
            }
        }
    }

    private void generarQRLocalUniversal(PacienteResponse paciente) {
        try {
            String qrContent = crearContenidoQRUniversal(paciente);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            ivCodigoQR.setImageBitmap(bitmap);
            ivCodigoQR.setVisibility(View.VISIBLE);

        } catch (WriterException e) {
            Log.e("QR_GEN", "Error generando QR local", e);
            mostrarQRSimple(paciente);
        } catch (Exception e) {
            Log.e("QR_GEN", "Error inesperado generando QR", e);
            mostrarQRSimple(paciente);
        }
    }

    private String crearContenidoQRUniversal(PacienteResponse paciente) {
        // Formato universal compatible con todos los lectores
        return "INFORMACIÓN DEL PACIENTE\n" +
                "-----------------------\n" +
                "ID: " + paciente.getId() + "\n" +
                "Nombre: " + paciente.getNombre() + " " + paciente.getApellido() + "\n" +
                "Identificación: " + (paciente.getIdentificacion() != null ? paciente.getIdentificacion() : "N/A") + "\n" +
                "Fecha Nacimiento: " + (paciente.getFecha_nacimiento() != null ? formatFecha(paciente.getFecha_nacimiento()) : "N/A") + "\n" +
                "Sexo: " + (paciente.getSexo() != null ? capitalizeFirstLetter(paciente.getSexo()) : "N/A") + "\n" +
                "Diagnóstico: " + (paciente.getDiagnostico_principal() != null ? paciente.getDiagnostico_principal() : "N/A") + "\n" +
                "-----------------------\n" +
                "Generado por la App QfindeR";
    }

    private void mostrarQRSimple(PacienteResponse paciente) {
        try {
            // Versión mínima de respaldo
            String qrContent = "Paciente:" + paciente.getId() + ":" +
                    paciente.getNombre() + ":" + paciente.getApellido();

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 400, 400);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            ivCodigoQR.setImageBitmap(bitmap);
            showToast("QR generado en formato simple");
        } catch (Exception e) {
            Log.e("QR_SIMPLE", "Error en QR simple", e);
            ivCodigoQR.setVisibility(View.GONE);
            showToast("No se pudo generar el código QR");
        }
    }

    private void displayPacienteData(PacienteResponse paciente) {
        if (!isAdded() || getActivity() == null || paciente == null) return;

        getActivity().runOnUiThread(() -> {
            String nombreCompleto = (paciente.getNombre() + " " + paciente.getApellido()).trim();
            tvNombreApellido.setText(nombreCompleto);

            String fecha = paciente.getFecha_nacimiento() != null ?
                    formatFecha(paciente.getFecha_nacimiento()) : "No especificada";
            tvFechaNacimiento.setText("FECHA DE NACIMIENTO: " + fecha);

            String sexo = paciente.getSexo() != null ? capitalizeFirstLetter(paciente.getSexo()) : "No especificado";
            tvSexo.setText("SEXO: " + sexo);

            String diagnostico = paciente.getDiagnostico_principal() != null ?
                    paciente.getDiagnostico_principal() : "Sin diagnóstico";
            tvDiagnostico.setText("DIAGNÓSTICO: " + diagnostico);

            String identificacion = paciente.getIdentificacion() != null ?
                    paciente.getIdentificacion() : "No especificada";
            tvIdentificacion.setText("IDENTIFICACIÓN: " + identificacion);
        });
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
    public void onPacienteActualizado() {
        loadPacienteData();
    }

    private String formatFecha(String fechaOriginal) {
        try {
            SimpleDateFormat original = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat formatoDeseado = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date fecha = original.parse(fechaOriginal);
            return formatoDeseado.format(fecha);
        } catch (Exception e) {
            return fechaOriginal;
        }
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private void showLoading(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    private void showError(String message) {
        if (!isAdded() || getActivity() == null) return;
        getActivity().runOnUiThread(() ->
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
    }

    private void showToast(String message) {
        if (!isAdded() || getActivity() == null) return;
        getActivity().runOnUiThread(() ->
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show());
    }

    private void navigateBack() {
        FragmentManager fragmentManager = getParentFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, new DashboardFragment());
            transaction.commit();
        }
    }
}