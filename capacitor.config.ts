import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.realstereo.dev',
  appName: 'Real Stereo - Sahagún',
  webDir: 'dist/real-stereo/browser',
  plugins: {
    BackgroundMode: {
      notificationTitle: 'Real Stereo',
      notificationText: 'Radio reproduciéndose en segundo plano',
      enableHighAccuracy: false,
      enableWebViewOptimizations: false
    },
    App: {
      launchUrl: null,
      iosScheme: 'realstereo'
    },
    SplashScreen: {
      launchAutoHide: true,        // ✅ CAMBIAR A true
      androidSplashResourceName: "splash",
      showSpinner: false,
      backgroundColor: "#1e3c72",  // ✅ AGREGAR color de fondo
      androidScaleType: "CENTER_CROP",
      splashFullScreen: true,      // ✅ AGREGAR pantalla completa
      splashImmersive: true        // ✅ AGREGAR modo inmersivo
    }
  }
};

export default config;
