package com.realstereo.dev;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

  private static final String TAG = "SplashActivity";
  private Handler handler;
  private Runnable splashRunnable;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d(TAG, "SplashActivity iniciada");

    // Ocultar splash de Capacitor inmediatamente
    hideSplashScreen();

    // Configurar transición con Handler más robusto
    handler = new Handler(Looper.getMainLooper());
    splashRunnable = new Runnable() {
      @Override
      public void run() {
        try {
          Log.d(TAG, "Iniciando transición a MainActivity");
          goToMainActivity();
        } catch (Exception e) {
          Log.e(TAG, "Error en transición: " + e.getMessage());
          // Reintentar después de 500ms si falla
          handler.postDelayed(this, 500);
        }
      }
    };

    // Esperar 1.5 segundos antes de continuar
    handler.postDelayed(splashRunnable, 1500);
  }

  private void hideSplashScreen() {
    try {
      Class<?> splashScreenClass = Class.forName("com.capacitorjs.plugins.splashscreen.SplashScreen");
      Object splashScreen = splashScreenClass.newInstance();
      splashScreenClass.getMethod("hide").invoke(splashScreen);
      Log.d(TAG, "Splash de Capacitor ocultado");
    } catch (Exception e) {
      Log.d(TAG, "No se pudo ocultar splash de Capacitor: " + e.getMessage());
    }
  }

  private void goToMainActivity() {
    if (!isFinishing() && !isDestroyed()) {
      Intent intent = new Intent(SplashActivity.this, MainActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(intent);

      // Terminar esta actividad después de un pequeño delay
      handler.postDelayed(new Runnable() {
        @Override
        public void run() {
          if (!isFinishing()) {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
          }
        }
      }, 100);

      Log.d(TAG, "Transición a MainActivity completada");
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (handler != null && splashRunnable != null) {
      handler.removeCallbacks(splashRunnable);
    }
    Log.d(TAG, "SplashActivity destruida");
  }

  @Override
  public void onBackPressed() {
    // Evitar que el usuario pueda salir del splash con el botón atrás
    super.onBackPressed(); // Llamar al método padre
    // Pero no hacer nada más para mantener el splash
  }
}
