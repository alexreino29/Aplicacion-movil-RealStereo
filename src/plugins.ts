import { Capacitor } from '@capacitor/core';

// Configuración para plugins de background
export const initializePlugins = async () => {
  if (Capacitor.isNativePlatform()) {
    try {
      // Importar dinámicamente el plugin de background mode
      const { BackgroundMode } = await import('@anuradev/capacitor-background-mode');

      // Hacer disponible globalmente
      (window as any).BackgroundMode = BackgroundMode;

      console.log('Plugins de background inicializados');
    } catch (error) {
      console.error('Error inicializando plugins:', error);
    }
  }
};
