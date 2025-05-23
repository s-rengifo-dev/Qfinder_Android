import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class QfinderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("QfinderApplication", "Initializing Firebase")
        try {
            FirebaseApp.initializeApp(this)
            Log.d("QfinderApplication", "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e("QfinderApplication", "Failed to initialize Firebase", e)
        }
    }
}