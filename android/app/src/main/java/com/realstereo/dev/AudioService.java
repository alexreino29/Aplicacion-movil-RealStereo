package com.realstereo.dev;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import android.util.Log;

public class AudioService extends Service implements
    MediaPlayer.OnPreparedListener,
    MediaPlayer.OnErrorListener,
    MediaPlayer.OnInfoListener,
    AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = "AudioService";
    private static final String CHANNEL_ID = "RADIO_CHANNEL";
    private static final int NOTIFICATION_ID = 1;

    private MediaPlayer mediaPlayer;
    private MediaSessionCompat mediaSession;
    private PowerManager.WakeLock wakeLock;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private boolean isPlaying = false;
    private boolean hasAudioFocus = false;
    private final String streamUrl = "https://stream.zeno.fm/oa7brrybk0vuv";

    public class AudioBinder extends Binder {
        public AudioService getService() {
            return AudioService.this;
        }
    }

    private final IBinder binder = new AudioBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "AudioService created");

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        createNotificationChannel();
        initializeWakeLock();
        initializeMediaSession();
        initializeMediaPlayer();
        initializeAudioFocus();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Real Stereo Radio",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Reproducción de radio en segundo plano");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setSound(null, null);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void initializeWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "RealStereo::AudioWakeLock"
            );
        }
    }

    private void initializeAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(this)
                .build();
        }
    }

    private void initializeMediaSession() {
        mediaSession = new MediaSessionCompat(this, TAG);
        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        );

        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                Log.d(TAG, "MediaSession: onPlay");
                startPlaying();
            }

            @Override
            public void onPause() {
                Log.d(TAG, "MediaSession: onPause");
                pausePlaying();
            }

            @Override
            public void onStop() {
                Log.d(TAG, "MediaSession: onStop");
                stopPlaying();
            }
        });

        mediaSession.setActive(true);
    }

    private void initializeMediaPlayer() {
        mediaPlayer = new MediaPlayer();

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build();
        mediaPlayer.setAudioAttributes(audioAttributes);

        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand received");

        // Mostrar notificación de carga inmediatamente SIEMPRE
        startForeground(NOTIFICATION_ID, createNotificationLoading());

        // Manejar botones de media
        MediaButtonReceiver.handleIntent(mediaSession, intent);

        if (intent != null) {
            String action = intent.getAction();
            if ("PLAY".equals(action)) {
                startPlaying();
            } else if ("PAUSE".equals(action)) {
                pausePlaying();
            } else if ("STOP".equals(action)) {
                stopPlaying();
            } else {
                // Auto-iniciar reproducción como estaba antes
                startPlaying();
            }
        } else {
            // Auto-iniciar reproducción como estaba antes
            startPlaying();
        }

        return START_STICKY;
    }

    private boolean requestAudioFocus() {
        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            result = audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }

        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        return hasAudioFocus;
    }

    private void abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            audioManager.abandonAudioFocus(this);
        }
        hasAudioFocus = false;
    }

    public void startPlaying() {
        if (isPlaying) {
            Log.d(TAG, "Already playing");
            return;
        }

        Log.d(TAG, "Starting playback");

        if (!requestAudioFocus()) {
            Log.w(TAG, "Could not get audio focus");
            return;
        }

        try {
            if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire(10*60*1000L);
            }

            mediaPlayer.reset();
            mediaPlayer.setDataSource(streamUrl);
            mediaPlayer.prepareAsync();

            isPlaying = true;
            updateMediaSessionState(PlaybackStateCompat.STATE_CONNECTING);

        } catch (Exception e) {
            Log.e(TAG, "Error starting playback", e);
            isPlaying = false;
            abandonAudioFocus();
        }
    }

    public void pausePlaying() {
        Log.d(TAG, "Pausing playback");

        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }

        isPlaying = false;
        updateMediaSessionState(PlaybackStateCompat.STATE_PAUSED);
        updateNotification();
        abandonAudioFocus();
    }

    public void stopPlaying() {
        Log.d(TAG, "Stopping playback");

        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
        }

        isPlaying = false;

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        abandonAudioFocus();
        updateMediaSessionState(PlaybackStateCompat.STATE_STOPPED);
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "MediaPlayer prepared, starting playback");
        if (hasAudioFocus) {
            mp.start();
            updateMediaSessionState(PlaybackStateCompat.STATE_PLAYING);
            // Actualizar notificación a "EN VIVO" cuando realmente esté sonando
            updateNotification();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);

        new android.os.Handler().postDelayed(() -> {
            if (isPlaying && hasAudioFocus) {
                Log.d(TAG, "Attempting to reconnect...");
                startPlaying();
            }
        }, 3000);

        return true;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "MediaPlayer info: what=" + what + ", extra=" + extra);
        return false;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.d(TAG, "Audio focus changed: " + focusChange);

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                hasAudioFocus = true;
                if (isPlaying && mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                    mediaPlayer.setVolume(1.0f, 1.0f);
                }
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                hasAudioFocus = false;
                pausePlaying();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                hasAudioFocus = false;
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.setVolume(0.3f, 0.3f);
                }
                break;
        }
    }

    // ✅ CAMBIO 1: createNotificationLoading() - Ahora usa ícono del sistema
    private Notification createNotificationLoading() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, flags);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Real Stereo Sahagún")
            .setContentText("⏳ Conectando...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build();
    }

    // ✅ CAMBIO 2: createNotification() - Ya estaba bien con ícono del sistema
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, flags);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Real Stereo Sahagún")
            .setContentText("🔴 EN VIVO - Radio reproduciéndose")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build();
    }

    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification());
        }
    }

    private void updateMediaSessionState(int state) {
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_STOP
            )
            .setState(state, 0, 1.0f);

        mediaSession.setPlaybackState(stateBuilder.build());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "AudioService destroyed");

        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        abandonAudioFocus();
        super.onDestroy();
    }

    public boolean isCurrentlyPlaying() {
        return isPlaying && mediaPlayer != null && mediaPlayer.isPlaying();
    }
}
