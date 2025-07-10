package com.sena.qfinder.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import android.Manifest;
import com.sena.qfinder.R;
import com.sena.qfinder.data.api.ApiClient;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.models.AgregarColaboradorRequest;
import com.sena.qfinder.data.models.PacienteListResponse;
import com.sena.qfinder.data.models.PacienteResponse;
import com.sena.qfinder.data.models.PerfilUsuarioResponse;
import com.sena.qfinder.data.models.RolResponse;
import com.sena.qfinder.data.models.UsuarioResponse;
import com.sena.qfinder.ui.paciente.EditarPacienteDialogFragment;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
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
    private static final int REQUEST_WRITE_STORAGE = 112;
    private static final String QR_FOLDER_NAME = "QFindeR_QRs";

    // Variables
    private int pacienteId;
    private SharedPreferences sharedPreferences;
    private AuthService authService;
    private PacienteResponse pacienteActual;
    private String currentImageData;
    private PerfilUsuarioResponse perfilUsuario;

    // Views
    private TextView tvNombreApellido, tvFechaNacimiento, tvSexo, tvDiagnostico, tvIdentificacion;
    private ImageView btnBack, ivCodigoQR, imagenPerfilP, btnEditarPaciente;
    private ProgressBar progressBar;
    private LinearLayout btnAgregarColaborador;
    private LinearLayout btnEliminarPaciente;
    private LinearLayout btnDescargarQR;

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

        if (savedInstanceState != null) {
            pacienteActual = savedInstanceState.getParcelable(KEY_PACIENTE);
            currentImageData = savedInstanceState.getString(KEY_IMAGE_DATA);
        }

        initializeRetrofit();
        cargarPerfilUsuario();
    }

    private void cargarPerfilUsuario() {
        String token = sharedPreferences.getString("token", null);
        if (token == null) {
            return;
        }

        authService.obtenerPerfil("Bearer " + token).enqueue(new Callback<PerfilUsuarioResponse>() {
            @Override
            public void onResponse(Call<PerfilUsuarioResponse> call, Response<PerfilUsuarioResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    perfilUsuario = response.body();
                    // Si ya tenemos datos del paciente, actualizamos el QR
                    if (pacienteActual != null) {
                        mostrarQRDelPaciente(pacienteActual);
                    }
                }
            }

            @Override
            public void onFailure(Call<PerfilUsuarioResponse> call, Throwable t) {
                Log.e("PerfilUsuario", "Error al cargar perfil", t);
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_PACIENTE, pacienteActual);
        outState.putString(KEY_IMAGE_DATA, currentImageData);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil_paciente, container, false);
        initViews(view);
        setupClickListeners(view);

        btnAgregarColaborador = view.findViewById(R.id.btnAgregarColaborador);
        btnAgregarColaborador.setOnClickListener(v -> mostrarDialogoAgregarColaborador(pacienteId));

        if (pacienteActual != null) {
            displayPacienteData(pacienteActual);
            mostrarQRDelPaciente(pacienteActual);
        } else {
            loadPacienteData();
        }
        verificarRolPaciente(pacienteId, "eliminar");
        verificarRolPaciente(pacienteId, "agregar_colaborador");
        verificarRolPaciente(pacienteId, "editar");

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (pacienteActual != null) {
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
        btnEliminarPaciente = view.findViewById(R.id.btnEliminarPaciente);
        btnEditarPaciente = view.findViewById(R.id.boton_imagen);
        btnDescargarQR = view.findViewById(R.id.btnDescargarQR);
    }

    private void setupClickListeners(View view) {
        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        btnEditarPaciente.setOnClickListener(v -> abrirDialogoEditar());
        btnEliminarPaciente.setOnClickListener(v -> mostrarDialogoConfirmacionEliminacion());
        btnDescargarQR.setOnClickListener(v -> descargarCodigoQR());
    }

    private void descargarCodigoQR() {
        if (pacienteActual == null) {
            showToast("No hay datos de paciente disponibles");
            return;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
        } else {
            procesarDescargaQR();
        }
    }

    private void procesarDescargaQR() {
        // Prioridad 1: Usar el QR de la API si está disponible
        if (pacienteActual.getQrCode() != null && !pacienteActual.getQrCode().isEmpty()) {
            try {
                String pureBase64 = pacienteActual.getQrCode().contains(",") ?
                        pacienteActual.getQrCode().substring(pacienteActual.getQrCode().indexOf(",") + 1) :
                        pacienteActual.getQrCode();
                byte[] decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT);
                Bitmap qrBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                // Añadir borde blanco para consistencia
                qrBitmap = addWhiteBorder(qrBitmap, 20);
                guardarQREnDispositivo(qrBitmap);
                return;
            } catch (Exception e) {
                Log.e("QR_DOWNLOAD", "Error al decodificar QR base64", e);
            }
        }

        // Prioridad 2: Usar el ImageView si tiene un QR
        if (ivCodigoQR.getDrawable() != null) {
            Bitmap qrBitmap = getBitmapFromView(ivCodigoQR);
            if (qrBitmap != null) {
                guardarQREnDispositivo(qrBitmap);
                return;
            }
        }

        // Prioridad 3: Generar QR localmente como último recurso
        Bitmap qrBitmap = generarQRBitmapLocal(pacienteActual);
        if (qrBitmap != null) {
            guardarQREnDispositivo(qrBitmap);
        } else {
            showToast("No se pudo generar el código QR para descargar");
        }
    }

    private Bitmap generarQRBitmapLocal(PacienteResponse paciente) {
        try {
            String qrContent = crearContenidoQRUniversal(paciente, perfilUsuario);
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);

            Bitmap bitmap = Bitmap.createBitmap(QR_CODE_SIZE, QR_CODE_SIZE, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < QR_CODE_SIZE; x++) {
                for (int y = 0; y < QR_CODE_SIZE; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return addWhiteBorder(bitmap, 20);
        } catch (WriterException e) {
            Log.e("QR_GEN", "Error generando QR", e);
            return null;
        }
    }

    private Bitmap addWhiteBorder(Bitmap bmp, int borderSize) {
        Bitmap bmpWithBorder = Bitmap.createBitmap(
                bmp.getWidth() + borderSize * 2,
                bmp.getHeight() + borderSize * 2,
                bmp.getConfig());
        Canvas canvas = new Canvas(bmpWithBorder);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(bmp, borderSize, borderSize, null);
        return bmpWithBorder;
    }

    private void guardarQREnDispositivo(Bitmap bitmap) {
        String nombrePaciente = pacienteActual != null ?
                pacienteActual.getNombre() + "_" + pacienteActual.getApellido() : "QR";
        String fileName = "QFindeR_" + nombrePaciente + "_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".png";

        // Intentar guardar en almacenamiento externo primero
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File qrFolder = new File(downloadsDir, QR_FOLDER_NAME);

        if (!qrFolder.exists() && !qrFolder.mkdirs()) {
            showToast("Error al crear directorio, guardando internamente");
            guardarEnAlmacenamientoInterno(bitmap, fileName);
            return;
        }

        File file = new File(qrFolder, fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();

            MediaScannerConnection.scanFile(
                    requireContext(),
                    new String[]{file.getAbsolutePath()},
                    new String[]{"image/png"},
                    (path, uri) -> new Handler(Looper.getMainLooper()).post(() -> {
                        showToast("QR guardado en: Descargas/" + QR_FOLDER_NAME);
                        Log.d("QR_SAVED", "Archivo guardado en: " + file.getAbsolutePath());
                    }));
        } catch (IOException e) {
            Log.e("QR_SAVE", "Error al guardar QR", e);
            guardarEnAlmacenamientoInterno(bitmap, fileName);
        }
    }

    private void guardarEnAlmacenamientoInterno(Bitmap bitmap, String fileName) {
        File internalDir = requireContext().getFilesDir();
        File qrFolder = new File(internalDir, QR_FOLDER_NAME);

        if (!qrFolder.exists() && !qrFolder.mkdirs()) {
            showToast("Error al crear directorio interno");
            return;
        }

        File file = new File(qrFolder, fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            showToast("QR guardado en almacenamiento interno");
        } catch (IOException e) {
            Log.e("QR_SAVE_INTERNAL", "Error al guardar QR internamente", e);
            showToast("Error al guardar el QR");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                new Handler().postDelayed(this::procesarDescargaQR, 300);
            } else {
                showToast("Permiso denegado. Se guardará en almacenamiento interno.");
                procesarDescargaQR();
            }
        }
    }

    private Bitmap getBitmapFromView(View view) {
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
        view.setDrawingCacheEnabled(false);
        return bitmap;
    }

    private void mostrarDialogoConfirmacionEliminacion() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar eliminación")
                .setMessage("¿Estás seguro de que deseas eliminar este paciente? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarPaciente())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void verificarRolPaciente(int pacienteId, String accion) {
        String token = sharedPreferences.getString("token", null);
        if (token == null) {
            showError("Sesión no válida");
            return;
        }

        authService.obtenerRolPaciente("Bearer " + token, pacienteId)
                .enqueue(new Callback<RolResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<RolResponse> call, @NonNull Response<RolResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String rol = response.body().getRol();
                            if ("responsable".equals(rol)) {
                                mostrarBotonSegunAccion(accion, true);
                            } else {
                                mostrarBotonSegunAccion(accion, false);
                            }
                        } else {
                            mostrarBotonSegunAccion(accion, false);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<RolResponse> call, @NonNull Throwable t) {
                        mostrarBotonSegunAccion(accion, false);
                    }

                    private void mostrarBotonSegunAccion(String accion, boolean visible) {
                        int visibilidad = visible ? View.VISIBLE : View.GONE;
                        switch (accion) {
                            case "eliminar":
                                btnEliminarPaciente.setVisibility(visibilidad);
                                break;
                            case "agregar_colaborador":
                                btnAgregarColaborador.setVisibility(visibilidad);
                                break;
                            case "editar":
                                btnEditarPaciente.setVisibility(visibilidad);
                                break;
                        }
                    }
                });
    }

    private void eliminarPaciente() {
        String token = sharedPreferences.getString("token", null);
        if (token == null) {
            showError("Sesión no válida");
            return;
        }

        showLoading(true);
        authService.eliminarPaciente("Bearer " + token, pacienteId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                showLoading(false);
                if (response.isSuccessful()) {
                    showToast("Paciente eliminado exitosamente");
                    requireActivity().onBackPressed();
                } else {
                    showError("Error al eliminar el paciente");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                showLoading(false);
                showError("Error de conexión: " + t.getMessage());
            }
        });
    }

    private void mostrarDialogoAgregarColaborador(int idPaciente) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.fragment_agregar_colaborador, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        EditText etCorreo = dialogView.findViewById(R.id.etCorreoColaborador);
        ImageView btnBuscar = dialogView.findViewById(R.id.btnBuscarColaborador);
        LinearLayout contenedor = dialogView.findViewById(R.id.contenedorColaborador);
        CheckBox checkbox = dialogView.findViewById(R.id.checkboxSeleccionarColaborador);
        TextView tvNombre = dialogView.findViewById(R.id.tvNombreColaborador);
        TextView tvApellido = dialogView.findViewById(R.id.tvApellidoColaborador);
        TextView tvCorreo = dialogView.findViewById(R.id.tvCorreoColaborador);
        Button btnAgregar = dialogView.findViewById(R.id.btnConfirmarAgregarColaborador);

        contenedor.setVisibility(View.GONE);
        btnAgregar.setVisibility(View.GONE);

        SharedPreferences preferences = requireContext().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = preferences.getString("token", null);

        if (token == null) {
            Toast.makeText(getContext(), "Token no encontrado", Toast.LENGTH_SHORT).show();
            return;
        }

        Retrofit retrofit = ApiClient.getClient();
        AuthService authService = retrofit.create(AuthService.class);

        final int[] idUsuarioColaborador = { -1 };

        btnBuscar.setOnClickListener(v -> {
            String correo = etCorreo.getText().toString().trim();
            if (correo.isEmpty()) {
                Toast.makeText(getContext(), "Ingresa un correo válido", Toast.LENGTH_SHORT).show();
                return;
            }

            Call<UsuarioResponse> call = authService.buscarColaboradorPorCorreo("Bearer " + token, correo);
            call.enqueue(new Callback<UsuarioResponse>() {
                @Override
                public void onResponse(Call<UsuarioResponse> call, Response<UsuarioResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        UsuarioResponse usuario = response.body();
                        contenedor.setVisibility(View.VISIBLE);
                        tvNombre.setText("Nombre: " + usuario.getNombre());
                        tvApellido.setText("Apellido: " + usuario.getApellido());
                        tvCorreo.setText("Correo: " + usuario.getCorreo());

                        idUsuarioColaborador[0] = usuario.getId();
                    } else {
                        contenedor.setVisibility(View.GONE);
                        btnAgregar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Colaborador no encontrado", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<UsuarioResponse> call, Throwable t) {
                    Toast.makeText(getContext(), "Error de conexión", Toast.LENGTH_SHORT).show();
                }
            });
        });

        checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnAgregar.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        btnAgregar.setOnClickListener(v -> {
            if (idUsuarioColaborador[0] == -1) {
                Toast.makeText(getContext(), "No se ha seleccionado un colaborador válido", Toast.LENGTH_SHORT).show();
                return;
            }

            AgregarColaboradorRequest request = new AgregarColaboradorRequest(idUsuarioColaborador[0], idPaciente);
            Call<ResponseBody> call = authService.agregarColaborador("Bearer " + token, request);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), "Colaborador agregado correctamente", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        try {
                            if (response.errorBody() != null) {
                                String errorBody = response.errorBody().string();

                                try {
                                    JSONObject jsonObject = new JSONObject(errorBody);
                                    String errorMsg = jsonObject.getString("error");

                                    // Validar el contenido del mensaje y mostrar snackbar con estilo
                                    if (errorMsg.contains("límite de 0 colaboradores")) {
                                        mostrarMensajeLimitePlan("free", 0, 0, "colaboradores");
                                    } else if (errorMsg.contains("límite de 5 colaboradores")) {
                                        mostrarMensajeLimitePlan("plus", 5, 5, "colaboradores");
                                    } else if (errorMsg.contains("límite de 2 colaboradores")) {
                                        mostrarMensajeLimitePlan("free", 2, 2, "colaboradores");
                                    } else {
                                        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                                    }
                                } catch (Exception e) {
                                    Toast.makeText(getContext(), "Error al procesar la respuesta", Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Toast.makeText(getContext(), "Error desconocido al agregar colaborador", Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "Error inesperado", Toast.LENGTH_LONG).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(getContext(), "Error al conectar con el servidor", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void mostrarMensajeLimitePlan(String planActual, int cantidadRegistrada, int limite, String tipo) {
        View view = getView();
        if (view != null) {
            Snackbar snackbar = Snackbar.make(view, "", Snackbar.LENGTH_INDEFINITE);
            Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbar.getView();
            layout.setPadding(0, 0, 0, 0);
            layout.setBackgroundColor(Color.TRANSPARENT);

            View customView = LayoutInflater.from(requireContext()).inflate(R.layout.snackbar_limit_exceeded, null);

            TextView message = customView.findViewById(R.id.snackbar_message);
            Button actionButton = customView.findViewById(R.id.snackbar_action);
            ImageView icon = customView.findViewById(R.id.snackbar_icon);

            String mensaje = String.format("Plan %s: %d/%d %s registrados. ¡Has alcanzado el límite!",
                    planActual.toUpperCase(), cantidadRegistrada, limite, tipo);
            message.setText(mensaje);
            actionButton.setText("Actualizar Plan");
            icon.setImageResource(R.drawable.premium);

            actionButton.setOnClickListener(v -> {
                snackbar.dismiss();
                Intent intent = new Intent(requireActivity(), SubscriptionActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            });

            layout.addView(customView, 0);
            snackbar.show();
        }
    }

    private static class ErrorResponse {
        private String error;
        private String plan_actual;
        private int pacientes_registrados;
        private int colaboradores_registrados;
        private int limite;

        public String getError() { return error; }
        public String getPlan_actual() { return plan_actual; }
        public int getPacientes_registrados() { return pacientes_registrados; }
        public int getColaboradores_registrados() { return colaboradores_registrados; }
        public int getLimite() { return limite; }
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
                            return;
                        }
                    }
                    showError("Paciente no encontrado");
                } else {
                    showError("Error al cargar datos del paciente");
                }
            }

            @Override
            public void onFailure(@NonNull Call<PacienteListResponse> call, @NonNull Throwable t) {
                showLoading(false);
                showError("Error de conexión: " + t.getMessage());
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

        imagenPerfilP.setImageDrawable(null);

        try {
            if (imageData.startsWith("http")) {
                String urlWithTimestamp = imageData + "?t=" + System.currentTimeMillis();
                Glide.with(requireContext())
                        .load(urlWithTimestamp)
                        .apply(new RequestOptions()
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .skipMemoryCache(true)
                                .circleCrop())
                        .into(imagenPerfilP);
            } else {
                String base64Image = imageData.contains(",") ?
                        imageData.substring(imageData.indexOf(",") + 1) : imageData;

                byte[] imageBytes = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                Glide.with(requireContext())
                        .load(bitmap)
                        .apply(new RequestOptions()
                                .circleCrop())
                        .into(imagenPerfilP);
            }
            currentImageData = imageData;
        } catch (Exception e) {
            imagenPerfilP.setImageResource(R.drawable.perfil_paciente);
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
            String pureBase64 = base64QR.contains(",") ?
                    base64QR.substring(base64QR.indexOf(",") + 1) : base64QR;
            byte[] decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

            // Añadir borde blanco como en la versión local
            Bitmap borderedBitmap = addWhiteBorder(bitmap, 20);
            ivCodigoQR.setImageBitmap(borderedBitmap);
            ivCodigoQR.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e("QR_API", "Error al mostrar QR de API", e);
            // Fallback a generación local
            generarQRLocalUniversal(pacienteActual);
        }
    }

    private void generarQRLocalUniversal(PacienteResponse paciente) {
        try {
            String qrContent = crearContenidoQRUniversal(paciente, perfilUsuario);
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
            showError("No se pudo generar el código QR");
        }
    }

    private String crearContenidoQRUniversal(PacienteResponse paciente, PerfilUsuarioResponse usuario) {
        return "Nombre: " + paciente.getNombre() + " " + paciente.getApellido() + "\n" +
                "Contacto: " + (usuario != null ? usuario.getTelefono_usuario() : "No disponible") + "\n" +
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
        pacienteActual = pacienteActualizado;
        currentImageData = pacienteActualizado.getImagen_paciente();

        new Handler(Looper.getMainLooper()).post(() -> {
            Glide.with(requireContext()).clear(imagenPerfilP);
            displayPacienteData(pacienteActualizado);
            mostrarQRDelPaciente(pacienteActualizado);
        });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
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