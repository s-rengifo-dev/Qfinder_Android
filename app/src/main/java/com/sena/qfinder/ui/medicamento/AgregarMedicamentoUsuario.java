package com.sena.qfinder.ui.medicamento;

import android.app.DatePickerDialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.sena.qfinder.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AgregarMedicamentoUsuario#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AgregarMedicamentoUsuario extends Fragment {

    private Spinner spinnerPatients, spinnerMedications;
    private LinearLayout layoutDatePicker;
    private TextView tvSelectedDates;
    private EditText etDosage, etDescription;
    private Button btnSave;

    private Calendar startDate, endDate;
    private boolean isSelectingStartDate = true;
    private SimpleDateFormat dateFormatter;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public AgregarMedicamentoUsuario() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AgregarMedicamentoUsuario.
     */
    // TODO: Rename and change types and number of parameters
    public static AgregarMedicamentoUsuario newInstance(String param1, String param2) {
        AgregarMedicamentoUsuario fragment = new AgregarMedicamentoUsuario();
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
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_agregar_medicamento_usuario, container, false);

        // Inicializar componentes
        spinnerPatients = view.findViewById(R.id.spinner_patients);
        spinnerMedications = view.findViewById(R.id.spinner_medications);
        layoutDatePicker = view.findViewById(R.id.layout_date_picker);
        tvSelectedDates = view.findViewById(R.id.tv_selected_dates);
        etDosage = view.findViewById(R.id.et_dosage);
        etDescription = view.findViewById(R.id.et_description);
        btnSave = view.findViewById(R.id.btn_save);

        // Configurar el formato de fecha
        dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // Configurar los Spinners con datos de ejemplo
        setupSpinners();

        // Configurar el selector de fechas
        setupDatePicker();

        // Configurar el botón de guardar
        btnSave.setOnClickListener(v -> savePrescription());

        return view;
    }

    private void setupSpinners() {
        // Datos de ejemplo para pacientes
        String[] patients = {"Jose Carlos", "Luis Guillermo"};
        ArrayAdapter<String> patientsAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                patients
        );
        patientsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPatients.setAdapter(patientsAdapter);

        // Datos de ejemplo para medicamentos
        String[] medications = {"Silazina 200mg", "Acetaminofen 100mg", "Ibuprofeno 150mg"};
        ArrayAdapter<String> medicationsAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                medications
        );
        medicationsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMedications.setAdapter(medicationsAdapter);
    }

    private void setupDatePicker() {
        layoutDatePicker.setOnClickListener(v -> showDatePickerDialog());
    }

    private void showDatePickerDialog() {
        final Calendar currentDate = Calendar.getInstance();
        int year = currentDate.get(Calendar.YEAR);
        int month = currentDate.get(Calendar.MONTH);
        int day = currentDate.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);

                    if (isSelectingStartDate) {
                        startDate = selectedDate;
                        isSelectingStartDate = false;
                        showDatePickerDialog(); // Mostrar inmediatamente el selector para la fecha fin
                    } else {
                        endDate = selectedDate;
                        isSelectingStartDate = true;
                        updateSelectedDatesText();
                    }
                },
                year, month, day
        );

        datePickerDialog.show();
    }

    private void updateSelectedDatesText() {
        if (startDate != null && endDate != null) {
            String startDateStr = dateFormatter.format(startDate.getTime());
            String endDateStr = dateFormatter.format(endDate.getTime());
            tvSelectedDates.setText(startDateStr + " - " + endDateStr);
        } else if (startDate != null) {
            String startDateStr = dateFormatter.format(startDate.getTime());
            tvSelectedDates.setText(startDateStr + " - Seleccionar fecha fin");
        }
    }

    private void savePrescription() {
        // Validar que todos los campos estén completos
        if (spinnerPatients.getSelectedItem() == null) {
            showToast("Selecciona un paciente");
            return;
        }

        if (spinnerMedications.getSelectedItem() == null) {
            showToast("Selecciona un medicamento");
            return;
        }

        if (startDate == null || endDate == null) {
            showToast("Selecciona las fechas de inicio y fin");
            return;
        }

        if (etDosage.getText().toString().trim().isEmpty()) {
            showToast("Ingresa la dosis");
            return;
        }

        // Obtener los datos seleccionados
        String patient = spinnerPatients.getSelectedItem().toString();
        String medication = spinnerMedications.getSelectedItem().toString();
        String dosage = etDosage.getText().toString();
        String description = etDescription.getText().toString();
        String startDateStr = dateFormatter.format(startDate.getTime());
        String endDateStr = dateFormatter.format(endDate.getTime());

        // Aquí iría la lógica para guardar la prescripción
        // Por ejemplo, enviar a una base de datos o API

        showToast("Prescripción guardada para " + patient);

        // Opcional: limpiar el formulario después de guardar
        clearForm();
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void clearForm() {
        spinnerPatients.setSelection(0);
        spinnerMedications.setSelection(0);
        startDate = null;
        endDate = null;
        tvSelectedDates.setText("Seleccionar fechas");
        etDosage.setText("");
        etDescription.setText("");
    }
}