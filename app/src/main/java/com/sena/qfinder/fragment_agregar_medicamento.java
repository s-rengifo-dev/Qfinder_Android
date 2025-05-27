package com.sena.qfinder;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.sena.qfinder.api.AuthService;
import com.sena.qfinder.models.MedicamentoRequest;
import com.sena.qfinder.models.MedicamentoResponse;
import com.sena.qfinder.models.RegisterRequest;
import com.sena.qfinder.models.RegisterResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link fragment_agregar_medicamento#newInstance} factory method to
 * create an instance of this fragment.
 */
public class fragment_agregar_medicamento extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    Button btnGuardarMedicamento;
    TextInputEditText edtNombreMedicamento;
    TextInputEditText edtDosis;
    TextInputEditText edtDescripcion;


    private ProgressDialog progressDialog;

    public fragment_agregar_medicamento() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment fragment_agregar_medicamento.
     */
    // TODO: Rename and change types and number of parameters
    public static fragment_agregar_medicamento newInstance(String param1, String param2) {
        fragment_agregar_medicamento fragment = new fragment_agregar_medicamento();
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
        View view = inflater.inflate(R.layout.fragment_agregar_medicamento, container, false);

        initViews(view);

        Spinner spinnerTipo = view.findViewById(R.id.spTipo);

        String[] tipos = {"psiquiatrico", "neurologico", "general", "otro"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, tipos);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Asignar adaptador al Spinner
        spinnerTipo.setAdapter(adapter);

        spinnerTipo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String tipoSeleccionado = parent.getItemAtPosition(position).toString();
                Toast.makeText(requireContext(), "Seleccionaste: " + tipoSeleccionado, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Puedes dejarlo vacío
            }
        });

        // Inflate the layout for this fragment
        return view;
    }

    private void initViews(View view) {
        edtNombreMedicamento = view.findViewById(R.id.edtNombreMedicamento);
        //edtDosis = view.findViewById(R.id.edtDosis);
        edtDescripcion = view.findViewById(R.id.edtDescripcion);
        btnGuardarMedicamento = view.findViewById(R.id.btnGuardarMedicamento);

        // Configurar ProgressDialog
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle("Registrando");
        progressDialog.setMessage("Por favor espere...");
        progressDialog.setCancelable(false);
    }
}