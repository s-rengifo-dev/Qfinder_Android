package com.sena.qfinder.data.models;

import android.os.Parcel;
import android.os.Parcelable;

public class PacienteResponse implements Parcelable {
    private int id;
    private String nombre;
    private String apellido;
    private String identificacion;
    private String fecha_nacimiento;
    private String sexo;
    private String diagnostico_principal;
    private boolean es_cuidador_principal;
    private String parentesco;
    private String qrCode;
    private String imagen_paciente;

    public PacienteResponse() {
    }

    // Parcelable constructor
    protected PacienteResponse(Parcel in) {
        id = in.readInt();
        nombre = in.readString();
        apellido = in.readString();
        identificacion = in.readString();
        fecha_nacimiento = in.readString();
        sexo = in.readString();
        diagnostico_principal = in.readString();
        es_cuidador_principal = in.readByte() != 0;
        parentesco = in.readString();
        qrCode = in.readString();
        imagen_paciente = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(nombre);
        dest.writeString(apellido);
        dest.writeString(identificacion);
        dest.writeString(fecha_nacimiento);
        dest.writeString(sexo);
        dest.writeString(diagnostico_principal);
        dest.writeByte((byte) (es_cuidador_principal ? 1 : 0));
        dest.writeString(parentesco);
        dest.writeString(qrCode);
        dest.writeString(imagen_paciente);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PacienteResponse> CREATOR = new Creator<PacienteResponse>() {
        @Override
        public PacienteResponse createFromParcel(Parcel in) {
            return new PacienteResponse(in);
        }

        @Override
        public PacienteResponse[] newArray(int size) {
            return new PacienteResponse[size];
        }
    };

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getIdentificacion() { return identificacion; }
    public void setIdentificacion(String identificacion) { this.identificacion = identificacion; }
    public String getFecha_nacimiento() { return fecha_nacimiento; }
    public void setFecha_nacimiento(String fecha_nacimiento) { this.fecha_nacimiento = fecha_nacimiento; }
    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }
    public String getDiagnostico_principal() { return diagnostico_principal; }
    public void setDiagnostico_principal(String diagnostico_principal) { this.diagnostico_principal = diagnostico_principal; }
    public boolean isEs_cuidador_principal() { return es_cuidador_principal; }
    public void setEs_cuidador_principal(boolean es_cuidador_principal) { this.es_cuidador_principal = es_cuidador_principal; }
    public String getParentesco() { return parentesco; }
    public void setParentesco(String parentesco) { this.parentesco = parentesco; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public String getImagen_paciente() { return imagen_paciente; }
    public void setImagen_paciente(String imagen_paciente) { this.imagen_paciente = imagen_paciente; }
}
