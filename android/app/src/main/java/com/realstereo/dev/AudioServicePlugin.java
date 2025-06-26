package com.realstereo.dev;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "AudioServicePlugin")
public class AudioServicePlugin extends Plugin {

    private static final String TAG = "AudioServicePlugin";
    private AudioService audioService;
    private boolean isServiceBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected");
            AudioService.AudioBinder binder = (AudioService.AudioBinder) service;
            audioService = binder.getService();
            isServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service disconnected");
            audioService = null;
            isServiceBound = false;
        }
    };

    @PluginMethod
    public void startService(PluginCall call) {
        Log.d(TAG, "Starting audio service");

        try {
            Context context = getContext();
            Intent serviceIntent = new Intent(context, AudioService.class);

            // Iniciar el servicio
            context.startForegroundService(serviceIntent);

            // Enlazar al servicio
            context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

            call.resolve();
        } catch (Exception e) {
            Log.e(TAG, "Error starting service", e);
            call.reject("Failed to start service: " + e.getMessage());
        }
    }

    @PluginMethod
    public void stopService(PluginCall call) {
        Log.d(TAG, "Stopping audio service");

        try {
            if (isServiceBound) {
                getContext().unbindService(serviceConnection);
                isServiceBound = false;
            }

            Intent serviceIntent = new Intent(getContext(), AudioService.class);
            getContext().stopService(serviceIntent);

            call.resolve();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping service", e);
            call.reject("Failed to stop service: " + e.getMessage());
        }
    }

    @PluginMethod
    public void startRadio(PluginCall call) {
        Log.d(TAG, "Start radio requested");

        try {
            Context context = getContext();
            Intent serviceIntent = new Intent(context, AudioService.class);
            context.startForegroundService(serviceIntent);
            call.resolve();
        } catch (Exception e) {
            Log.e(TAG, "Error starting radio", e);
            call.reject("Failed to start radio: " + e.getMessage());
        }
    }

    @PluginMethod
    public void stopRadio(PluginCall call) {
        Log.d(TAG, "Stop radio requested");

        try {
            if (audioService != null) {
                audioService.stopPlaying();
            } else {
                Intent stopIntent = new Intent(getContext(), AudioService.class);
                getContext().stopService(stopIntent);
            }
            call.resolve();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping radio", e);
            call.reject("Failed to stop radio: " + e.getMessage());
        }
    }

    @PluginMethod
    public void playRadio(PluginCall call) {
        Log.d(TAG, "Play radio requested");

        if (audioService != null) {
            audioService.startPlaying();
            call.resolve();
        } else {
            call.reject("Service not available");
        }
    }

    @PluginMethod
    public void pauseRadio(PluginCall call) {
        Log.d(TAG, "Pause radio requested");

        if (audioService != null) {
            audioService.pausePlaying();
            call.resolve();
        } else {
            call.reject("Service not available");
        }
    }

    @PluginMethod
    public void isPlaying(PluginCall call) {
        if (audioService != null) {
            boolean playing = audioService.isCurrentlyPlaying();
            call.resolve(new com.getcapacitor.JSObject().put("isPlaying", playing));
        } else {
            call.resolve(new com.getcapacitor.JSObject().put("isPlaying", false));
        }
    }

    @Override
    protected void handleOnDestroy() {
        if (isServiceBound) {
            getContext().unbindService(serviceConnection);
            isServiceBound = false;
        }
        super.handleOnDestroy();
    }
}
