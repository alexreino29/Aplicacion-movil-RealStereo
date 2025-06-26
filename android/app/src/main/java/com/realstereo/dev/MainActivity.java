package com.realstereo.dev;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

  private static final String TAG = "MainActivity";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Log.d(TAG, "MainActivity iniciada");

    // Asegurar que el splash de Capacitor esté oculto
    hideSplashScreen();

    // Pedir permiso de notificación
    requestNotificationPermission();

    // Registrar el plugin
    registerPlugin(AudioServicePlugin.class);

    // RETRASAR el inicio del AudioService para evitar conflictos
    Handler handler = new Handler(Looper.getMainLooper());
    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        startAudioService();
      }
    }, 1000); // Esperar 1 segundo después de que MainActivity esté lista
  }

  private void hideSplashScreen() {
    try {
      Class<?> splashScreenClass = Class.forName("com.capacitorjs.plugins.splashscreen.SplashScreen");
      Object splashScreen = splashScreenClass.newInstance();
      splashScreenClass.getMethod("hide").invoke(splashScreen);
      Log.d(TAG, "Splash de Capacitor ocultado en MainActivity");
    } catch (Exception e) {
      Log.d(TAG, "No se pudo ocultar splash de Capacitor: " + e.getMessage());
    }
  }

  private void startAudioService() {
    try {
      Intent audioIntent = new Intent(this, AudioService.class);

      // Verificar versión de Android para usar el método correcto
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        // API 26+ usar startForegroundService
        startForegroundService(audioIntent);
        Log.d(TAG, "AudioService iniciado con startForegroundService (API 26+)");
      } else {
        // API 23-25 usar startService normal
        startService(audioIntent);
        Log.d(TAG, "AudioService iniciado con startService (API 23-25)");
      }
    } catch (Exception e) {
      Log.e(TAG, "Error al iniciar AudioService: " + e.getMessage());
    }
  }

  private void requestNotificationPermission() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
      != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(
        this,
        new String[]{Manifest.permission.POST_NOTIFICATIONS},
        1
      );
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    // Asegurar que el splash esté oculto al volver a la app
    hideSplashScreen();
  }
}
