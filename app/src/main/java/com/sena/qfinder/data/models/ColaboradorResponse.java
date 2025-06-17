package com.sena.qfinder.data.models;

public class ColaboradorResponse {
        private int id_usuario;
        private String nombre;
        private String apellido;
        private String correo;
        private String imagen_usuario;
        private int id_paciente;
        private String paciente;

        public int getId_usuario() {
            return id_usuario;
        }

        public String getNombre() {
            return nombre;
        }

        public String getApellido() {
            return apellido;
        }

        public String getCorreo() {
            return correo;
        }

        public String getImagen_usuario() {
            return imagen_usuario;
        }

        public int getId_paciente() {
            return id_paciente;
        }

        public String getNombre_paciente() {
            return paciente;
        }

}
