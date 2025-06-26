import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';
import { initializePlugins } from './plugins';
import { Capacitor } from '@capacitor/core';

// Registrar el plugin unificado globalmente
if (Capacitor.isNativePlatform()) {
  import('./plugins').then(({ initializePlugins }) => {
    initializePlugins();

    // Forzar registro del AudioServicePlugin unificado
    (window as any).AudioServicePlugin = {
      startService: () => (window as any).Capacitor.Plugins.AudioServicePlugin.startService(),
      stopService: () => (window as any).Capacitor.Plugins.AudioServicePlugin.stopService(),
      startRadio: () => (window as any).Capacitor.Plugins.AudioServicePlugin.startRadio(),
      stopRadio: () => (window as any).Capacitor.Plugins.AudioServicePlugin.stopRadio(),
      playRadio: () => (window as any).Capacitor.Plugins.AudioServicePlugin.playRadio(),
      pauseRadio: () => (window as any).Capacitor.Plugins.AudioServicePlugin.pauseRadio(),
      isPlaying: () => (window as any).Capacitor.Plugins.AudioServicePlugin.isPlaying()
    };
  });
}

// Inicializar plugins
initializePlugins();

bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error(err));
