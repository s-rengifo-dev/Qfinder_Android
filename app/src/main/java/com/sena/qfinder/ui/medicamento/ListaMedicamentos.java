package com.sena.qfinder.ui.medicamento;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.sena.qfinder.R;
import com.sena.qfinder.data.api.ApiClient;
import com.sena.qfinder.data.api.AuthService;
import com.sena.qfinder.data.models.MedicamentoRequest;
import com.sena.qfinder.data.models.MedicamentoResponse;
import com.sena.qfinder.data.models.MedicamentoSimpleResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ListaMedicamentos#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ListaMedicamentos extends Fragment {
    private TableLayout tablaMedicamentos;
    private Button btnAgregarMedicamento;

    Spinner tipoDato;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ListaMedicamentos() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ListaMedicamentos.
     */
    // TODO: Rename and change types and number of parameters
    public static ListaMedicamentos newInstance(String param1, String param2) {
        ListaMedicamentos fragment = new ListaMedicamentos();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lista_medicamentos, container, false);

        tablaMedicamentos = view.findViewById(R.id.tablaMedicamentos);
        btnAgregarMedicamento = view.findViewById(R.id.btnAgregarMedicamento);

        // Datos de prueba
        MedicamentoResponse medicamento = new MedicamentoResponse("Silazina", "200mg", "Tomar una pastilla al día");
        agregarFila(medicamento);

        btnAgregarMedicamento.setOnClickListener(v -> {
            // Aquí puedes abrir un diálogo o nueva pantalla para ingresar datos
            //agregarFila("Nuevo", "0mg", "Descripción");

            mostrarDialogoAgregarMedicamento();
        });

        obtenerMedicamentos();

        // Inflate the layout for this fragment
        return view;
    }

    private void mostrarDialogoAgregarMedicamento() {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View viewInflated = LayoutInflater.from(getContext()).inflate(R.layout.fragment_agregar_medicamento, (ViewGroup) getView(), false);
        builder.setView(viewInflated);

        TextInputEditText editTextNombre = viewInflated.findViewById(R.id.edtNombreMedicamento);
        //TextInputEditText editTextDosis = viewInflated.findViewById(R.id.edtDosis);
        Spinner tipo = viewInflated.findViewById(R.id.spTipo);
        TextInputEditText editTextDescripcion = viewInflated.findViewById(R.id.edtDescripcion);
        Button btnCancelar = viewInflated.findViewById(R.id.btnCancelar);
        Button btnGuardar = viewInflated.findViewById(R.id.btnGuardarMedicamento);

        AlertDialog dialog = builder.create();
        dialog.show();

        Spinner tipoSS = viewInflated.findViewById(R.id.spTipo);

        // Lista de opciones para el spinner
        String[] opciones = {"psiquiatrico", "neurologico", "general", "otro"}; // Cambia según tus tipos reales

        // Adaptador para el spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, opciones);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tipoSS.setAdapter(adapter);

        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nombre = editTextNombre.getText().toString().trim();
                String tipoDD = tipo.getSelectedItem().toString();
                String descripcion = editTextDescripcion.getText().toString().trim();

                if (!nombre.isEmpty() && !tipoDD.isEmpty()) {
                    MedicamentoResponse nuevoMedicamento = new MedicamentoResponse(nombre, tipoDD, descripcion);
                    agregarFila(nuevoMedicamento);
                    dialog.dismiss();
                    Toast.makeText(getContext(), "Medicamento agregado", Toast.LENGTH_SHORT).show();
                    // Aquí podrías guardar la información en una base de datos o lista


                    Log.d("AgregarMedicamento", "Nombre: " + nombre);
                    Log.d("AgregarMedicamento", "Tipo: " + tipoDD);
                    Log.d("AgregarMedicamento", "Descripción: " + descripcion);

                    // Crear objeto de solicitud
                    MedicamentoRequest request = new MedicamentoRequest(
                            nombre,
                            descripcion,
                            tipoDD
                    );

                    Log.d("AgregarMedicamento", "Request Nombre: " + request.getNombre());
                    Log.d("AgregarMedicamento", "Request Tipo: " + request.getTipo());
                    Log.d("AgregarMedicamento", "Request Descripción: " + request.getDescripcion());

                    SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("usuario", Context.MODE_PRIVATE);
                    String token = sharedPreferences.getString("token", null);


                    if (token == null || token.isEmpty()) {
                        Toast.makeText(getContext(), "Token no encontrado. Inicia sesión nuevamente.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Configurar Retrofit
                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl("https://qfinder-production.up.railway.app/")
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();

                    AuthService authService = retrofit.create(AuthService.class);

                    // Realizar la llamada al servidor
                    Call<MedicamentoResponse> call = authService.agregarMedicamento("Bearer " + token, request);
                    call.enqueue(new Callback<MedicamentoResponse>() {
                        @Override
                        public void onResponse(Call<MedicamentoResponse> call, Response<MedicamentoResponse> response) {

                            Log.d("AgregarMedicamento", "Código de respuesta: " + response.code());

                            if (response.isSuccessful()) {
                                Log.d("AgregarMedicamento", "Respuesta exitosa del servidor: " + response.body().getMessage());
                                Toast.makeText(getContext(), "Se registro el medicamento correctamente", Toast.LENGTH_SHORT).show();

                            } else {
                                Log.d("AgregarMedicamento", "Error en la respuesta del servidor: " + response.code());
                                try {
                                    Log.d("AgregarMedicamento", "Cuerpo del error: " + response.errorBody().string());
                                } catch (Exception e) {
                                    Log.d("AgregarMedicamento", "Error al leer el cuerpo del error: " + e.getMessage());
                                }
                                Toast.makeText(getContext(), "No se registro el medicamento correctamente", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<MedicamentoResponse> call, Throwable t) {

                            Log.e("AgregarMedicamento", "Error de conexión: " + t.getMessage());
                            Toast.makeText(getContext(), "Error de conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(getContext(), "El nombre y la dosis son obligatorios", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    private void limpiarTabla() {
        // Preserva la fila de encabezado (índice 0), elimina las demás
        int childCount = tablaMedicamentos.getChildCount();
        if (childCount > 1) {
            tablaMedicamentos.removeViews(1, childCount - 1);
        }
    }

    private void obtenerMedicamentos() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("usuario", Context.MODE_PRIVATE);
        String token = sharedPreferences.getString("token", null);

        if (token == null || token.isEmpty()) {
            Toast.makeText(getContext(), "Token no encontrado. Inicia sesión nuevamente.", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthService apiService = ApiClient.getClient().create(AuthService.class);
        Call<List<MedicamentoResponse>> call = apiService.listarMedicamentos("Bearer " + token);

        call.enqueue(new Callback<List<MedicamentoResponse>>() {
            @Override
            public void onResponse(Call<List<MedicamentoResponse>> call, Response<List<MedicamentoResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (MedicamentoResponse m : response.body()) {
                        agregarFila(m);
                    }
                    Log.d("MDDD3", "Descripción: " + response.body());
                } else {
                    Toast.makeText(getContext(), "Error al obtener medicamentos", Toast.LENGTH_SHORT).show();
                }

                Log.d("MDDD3", "Descripción: " + response.body());
            }

            @Override
            public void onFailure(Call<List<MedicamentoResponse>> call, Throwable t) {
                Toast.makeText(getContext(), "Fallo en la conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    private TextView createTextView(String text, boolean isHeader) {
        TextView textView = new TextView(getContext());
        textView.setText(text);
        textView.setPadding(8, 8, 8, 8);
        if (isHeader) {
            textView.setTextColor(Color.BLACK);
            textView.setTextSize(16);
            textView.setTypeface(textView.getTypeface(), android.graphics.Typeface.BOLD);
        }
        textView.setLayoutParams(new TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, isHeader ? 1 : 1));
        return textView;
    }

    private void agregarFila(MedicamentoResponse medicamento) {
        TableRow fila = new TableRow(getContext());

        Log.d("MDDD2", medicamento.toString());

        TextView tvNombre = createTextView(medicamento.getNombre(), false);
        TextView tvDosis = createTextView(medicamento.getTipo(), false);
        TextView tvDescripcion = createTextView(medicamento.getDescripcion(), false);

        ImageButton btnEliminar = new ImageButton(getContext());
        btnEliminar.setImageResource(android.R.drawable.ic_menu_delete);
        btnEliminar.setBackgroundColor(Color.TRANSPARENT);
        int padding = (int) getResources().getDimension(R.dimen.padding_eliminar_boton);
        btnEliminar.setPadding(padding, padding, padding, padding);
        TableRow.LayoutParams btnParams = new TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.gravity = Gravity.CENTER;
        btnEliminar.setLayoutParams(btnParams);

        // ACCION PARA EL BOTON DE ELIMINAR CADA MEDICAMENTO
        btnEliminar.setOnClickListener(v -> {
            //Toast.makeText(getContext(), "Eliminar " + medicamento.getNombre(), Toast.LENGTH_SHORT).show();
            // Aquí puedes agregar lógica para eliminar el medicamento del servidor y de la UI

            SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("usuario", Context.MODE_PRIVATE);
            String token = sharedPreferences.getString("token", null);

            if (token == null || token.isEmpty()) {
                Toast.makeText(getContext(), "Token no encontrado. Inicia sesión nuevamente.", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(getContext(), "ID: "+medicamento.getId_medicamento(), Toast.LENGTH_SHORT).show();

            AuthService apiService = ApiClient.getClient().create(AuthService.class);
            Call<MedicamentoSimpleResponse> call = apiService.eliminarMedicamento("Bearer " + token, medicamento.getId_medicamento());




            call.enqueue(new Callback<MedicamentoSimpleResponse>() {
                @Override
                public void onResponse(Call<MedicamentoSimpleResponse> call, Response<MedicamentoSimpleResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Toast.makeText(getContext(), response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        // Opcional: refrescar la lista
                        limpiarTabla(); // Limpia la tabla
                        obtenerMedicamentos();
                    } else {
                        Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<MedicamentoSimpleResponse> call, Throwable t) {
                    Toast.makeText(getContext(), "Fallo en la conexión: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });





        });

        fila.addView(tvNombre);
        fila.addView(tvDosis);
        fila.addView(tvDescripcion);
        fila.addView(btnEliminar);

        tablaMedicamentos.addView(fila);
    }
}