# QfindeR Android App

Cliente móvil nativo para el ecosistema QfindeR. Aplicación enfocada en la gestión de salud, comunicación en tiempo real y transacciones seguras.

### 🛠 Tech Stack
*   **Lenguaje:** Java / Kotlin
*   **Comunicación:** Retrofit / Coroutines (Consumo REST API)
*   **Backend Real-time:** Firebase (Firestore, Auth, Cloud Messaging)
*   **Pagos:** Mercado Pago SDK
*   **Arquitectura:** MVVM (Model-View-ViewModel)

### 🚀 Características Clave
*   **Gestión de Salud:** Visualización de notas médicas y registros de síntomas.
*   **Chat en Tiempo Real:** Implementación nativa con Firebase Firestore para comunicación entre paciente y personal médico.
*   **Pagos Integrados:** Procesamiento seguro de transacciones mediante Mercado Pago.
*   **Autenticación:** Integración con el backend (JWT) y Firebase Auth.

### ⚙️ Configuración e Instalación
1. Clona el repositorio: `git clone [[url](https://github.com/s-rengifo-dev/Qfinder_Android.git)]`
2. Configura los servicios:
   - Añade tu archivo `google-services.json` (Firebase) en la carpeta `app/`.
   - Crea un archivo `local.properties` y añade tus claves de API:
     ```
     MP_ACCESS_TOKEN=tu_token_mercado_pago
     BASE_URL=http://tu-backend-url:3000
     ```
3. Sincroniza con Gradle.
4. Ejecuta en tu dispositivo o emulador.

### 🔒 Seguridad
* *Nota:* Este proyecto implementa validación básica de certificados. No incluir nunca archivos con credenciales reales (`google-services.json`, `.env`, keys) en el repositorio público.

### 📸 Evidencia (Screenshots)

---
*Construyendo una solución móvil segura y eficiente.*
