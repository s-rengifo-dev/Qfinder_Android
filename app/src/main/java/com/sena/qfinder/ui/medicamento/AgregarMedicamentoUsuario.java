package com.sena.qfinder.ui.medicamento;

import android.app.DatePickerDialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.sena.qfinder.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AgregarMedicamentoUsuario extends Fragment {

    private Spinner spinnerPatients, spinnerMedications;
    private LinearLayout layoutStartDate, layoutEndDate;
    private TextView tvStartDate, tvEndDate;
    private EditText etDosage, etDescription;
    private Button btnSave;

    private Calendar startDate, endDate;
    private SimpleDateFormat dateFormatter;

    public AgregarMedicamentoUsuario() {
        // Required empty public constructor
    }

    public static AgregarMedicamentoUsuario newInstance(String param1, String param2) {
        AgregarMedicamentoUsuario fragment = new AgregarMedicamentoUsuario();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_agregar_medicamento_usuario, container, false);

        // Inicializar componentes
        spinnerPatients = view.findViewById(R.id.spinner_patients);
        spinnerMedications = view.findViewById(R.id.spinner_medications);
        layoutStartDate = view.findViewById(R.id.layout_start_date);
        layoutEndDate = view.findViewById(R.id.layout_end_date);
        tvStartDate = view.findViewById(R.id.tv_start_date);
        tvEndDate = view.findViewById(R.id.tv_end_date);
        etDosage = view.findViewById(R.id.et_dosage);
        etDescription = view.findViewById(R.id.et_description);
        btnSave = view.findViewById(R.id.btn_save);

        setupSpinners();
        setupDatePickers();

        btnSave.setOnClickListener(v -> savePrescription());

        return view;
    }

    private void setupSpinners() {
        String[] patients = {"Jose Carlos", "Luis Guillermo"};
        ArrayAdapter<String> patientsAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                patients
        );
        patientsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPatients.setAdapter(patientsAdapter);

        String[] medications = {"Silazina 200mg", "Acetaminofen 100mg", "Ibuprofeno 150mg"};
        ArrayAdapter<String> medicationsAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                medications
        );
        medicationsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMedications.setAdapter(medicationsAdapter);
    }

    private void setupDatePickers() {
        layoutStartDate.setOnClickListener(v -> showDatePickerDialog(true));
        layoutEndDate.setOnClickListener(v -> showDatePickerDialog(false));
    }

    private void showDatePickerDialog(boolean isStartDate) {
        final Calendar currentDate = Calendar.getInstance();
        int year = currentDate.get(Calendar.YEAR);
        int month = currentDate.get(Calendar.MONTH);
        int day = currentDate.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);

                    if (isStartDate) {
                        startDate = selectedDate;
                        tvStartDate.setText(dateFormatter.format(startDate.getTime()));
                    } else {
                        endDate = selectedDate;
                        tvEndDate.setText(dateFormatter.format(endDate.getTime()));
                    }
                },
                year, month, day
        );

        datePickerDialog.show();
    }

    private void savePrescription() {
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
        if (etDescription.getText().toString().trim().isEmpty()) {
            showToast("Ingresa la Frecuencia");
            return;
        }

        String patient = spinnerPatients.getSelectedItem().toString();
        String medication = spinnerMedications.getSelectedItem().toString();
        String dosage = etDosage.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String startDateStr = dateFormatter.format(startDate.getTime());
        String endDateStr = dateFormatter.format(endDate.getTime());

        // Aquí iría la lógica para guardar en base de datos/API
        showToast("Prescripción guardada para " + patient);

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
        tvStartDate.setText("Seleccionar fecha de inicio");
        tvEndDate.setText("Seleccionar fecha de fin");
        etDosage.setText("");
        etDescription.setText("");
    }
}
