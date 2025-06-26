import { Injectable } from '@angular/core';
import { App } from '@capacitor/app';
import { Capacitor } from '@capacitor/core';

// Plugin unificado
declare var AudioServicePlugin: any;

@Injectable({
 providedIn: 'root'
})
export class AudioService {
 private isUsingNativeService = false;
 private audio: HTMLAudioElement | null = null;
 private streamUrl = 'https://stream.zeno.fm/oa7brrybk0vuv';

 constructor() {
   this.initializeNativeFeatures();
 }

 private async initializeNativeFeatures() {
   if (Capacitor.isNativePlatform()) {
     try {
       // Verificar si el plugin unificado está disponible
       if (typeof AudioServicePlugin !== 'undefined') {
         this.isUsingNativeService = true;
         console.log('Usando servicio nativo unificado');

         // Inicializar el servicio
         await AudioServicePlugin.startService();
       } else {
         console.log('Plugin nativo no disponible, usando fallback');
         this.isUsingNativeService = false;
       }

       // Configurar listeners de la app
       App.addListener('appStateChange', ({ isActive }) => {
         console.log('App state changed:', isActive ? 'active' : 'background');

         if (!isActive) {
           this.handleAppGoingToBackground();
         } else {
           this.handleAppComingToForeground();
         }
       });

     } catch (error) {
       console.error('Error inicializando funciones nativas:', error);
       this.isUsingNativeService = false;
     }
   }
 }

 private async handleAppGoingToBackground() {
   console.log('App yendo a segundo plano');

   if (this.isUsingNativeService) {
     // El servicio nativo maneja automáticamente el segundo plano
     console.log('Servicio nativo mantendrá reproducción en segundo plano');
   } else {
     // Fallback para navegador/webview
     console.log('Usando fallback para segundo plano');
     this.setupWebAudioForBackground();
   }
 }

 private async handleAppComingToForeground() {
   console.log('App volviendo al primer plano');

   if (this.isUsingNativeService) {
     // Verificar estado del servicio nativo
     try {
       const result = await AudioServicePlugin.isPlaying();
       console.log('Estado del servicio nativo:', result.isPlaying);
     } catch (error) {
       console.error('Error verificando estado del servicio:', error);
     }
   }
 }

 async playRadio(callback?: () => void) {
   try {
     console.log('Iniciando reproducción...');

     if (this.isUsingNativeService) {
       // Usar el plugin unificado
       await AudioServicePlugin.startRadio();
       console.log('Reproducción nativa iniciada');

       // Para servicio nativo, ejecutar callback inmediatamente si existe
       if (callback) callback();

     } else {
       await this.playWebAudio();

       // Para audio web, ejecutar callback después de iniciar
       if (callback) callback();
     }

   } catch (error) {
     console.error('Error al reproducir:', error);
     if (callback) callback(); // Ejecutar callback incluso en error
   }
 }

 async stopRadio() {
   console.log('Deteniendo reproducción...');

   if (this.isUsingNativeService) {
     try {
       await AudioServicePlugin.stopRadio();
       console.log('Servicio nativo detenido');
     } catch (error) {
       console.error('Error deteniendo servicio nativo:', error);
     }
   }

   // Detener web audio también
   if (this.audio) {
     this.audio.pause();
     this.audio.currentTime = 0;
     this.audio.src = '';
     this.audio = null;

     if ('mediaSession' in navigator) {
       navigator.mediaSession.playbackState = 'none';
     }
   }

   console.log('Reproducción detenida completamente');
 }

 private async playWebAudio() {
   if (this.audio) {
     this.stopRadio();
   }

   this.audio = new Audio(this.streamUrl);
   this.audio.autoplay = true;
   this.audio.loop = true;
   this.audio.preload = 'auto';
   this.audio.volume = 1.0;

   // Configurar eventos del audio
   this.setupAudioEvents();

   await this.audio.play();
   this.setupMediaSession();

   console.log('Reproducción web iniciada exitosamente');
 }

 private setupAudioEvents() {
   if (!this.audio) return;

   this.audio.addEventListener('loadstart', () => {
     console.log('Iniciando carga del stream...');
   });

   this.audio.addEventListener('canplay', () => {
     console.log('Stream listo para reproducir');
   });

   this.audio.addEventListener('playing', () => {
     console.log('Audio reproduciéndose');
   });

   this.audio.addEventListener('pause', () => {
     console.log('Audio pausado');
   });

   this.audio.addEventListener('ended', () => {
     console.log('Audio terminado - reintentando...');
     this.reconnectWebAudio();
   });

   this.audio.addEventListener('error', (e) => {
     console.error('Error en el stream:', e);
     this.handleWebAudioError();
   });

   this.audio.addEventListener('stalled', () => {
     console.warn('Stream estancado - reintentando...');
     setTimeout(() => this.reconnectWebAudio(), 2000);
   });
 }

 private handleWebAudioError() {
   console.log('Manejando error de audio web - reintentando...');
   setTimeout(() => {
     this.reconnectWebAudio();
   }, 3000);
 }

 private reconnectWebAudio() {
   if (this.audio) {
     try {
       this.audio.load();
       this.audio.play().catch((error) => {
         console.error('Error en reconexión:', error);
         this.handleWebAudioError();
       });
     } catch (error) {
       console.error('Error reconectando:', error);
     }
   }
 }

 private setupWebAudioForBackground() {
   
   if (this.audio) {
     
     this.audio.setAttribute('crossorigin', 'anonymous');

     
     if ('wakeLock' in navigator) {
       (navigator as any).wakeLock.request('screen').catch((error: any) => {
         console.log('Wake lock no disponible:', error);
       });
     }
   }
 }

 async pauseRadio() {
   console.log('Pausando reproducción...');

   if (this.isUsingNativeService) {
     try {
       await AudioServicePlugin.pauseRadio();
     } catch (error) {
       console.error('Error pausando servicio nativo:', error);
     }
   }

   if (this.audio && !this.audio.paused) {
     this.audio.pause();
   }
 }

 async resumeRadio() {
   console.log('Reanudando reproducción...');

   if (this.isUsingNativeService) {
     try {
       await AudioServicePlugin.playRadio();
     } catch (error) {
       console.error('Error reanudando servicio nativo:', error);
     }
   }

   if (this.audio && this.audio.paused) {
     this.audio.play().catch(error => {
       console.error('Error reanudando:', error);
       this.reconnectWebAudio();
     });
   }
 }

 private setupMediaSession() {
   if ('mediaSession' in navigator) {
     navigator.mediaSession.metadata = new MediaMetadata({
       title: 'Real Stereo',
       artist: 'Radio en vivo',
       album: 'Real Stereo',
       artwork: [
         { src: 'assets/icon/icon.png', sizes: '96x96', type: 'image/png' },
         { src: 'assets/icon/icon.png', sizes: '128x128', type: 'image/png' },
         { src: 'assets/icon/icon.png', sizes: '192x192', type: 'image/png' },
         { src: 'assets/icon/icon.png', sizes: '256x256', type: 'image/png' },
         { src: 'assets/icon/icon.png', sizes: '384x384', type: 'image/png' },
         { src: 'assets/icon/icon.png', sizes: '512x512', type: 'image/png' }
       ]
     });

     navigator.mediaSession.setActionHandler('play', () => {
       this.resumeRadio();
     });

     navigator.mediaSession.setActionHandler('pause', () => {
       this.pauseRadio();
     });

     navigator.mediaSession.setActionHandler('stop', () => {
       this.stopRadio();
     });

     navigator.mediaSession.playbackState = 'playing';
     console.log('Media session configurada');
   }
 }


 async isPlaying(): Promise<boolean> {
   if (this.isUsingNativeService) {
     try {
       const result = await AudioServicePlugin.isPlaying();
       return result.isPlaying;
     } catch (error) {
       console.error('Error verificando estado nativo:', error);
       return false;
     }
   }

   
   if (this.audio) {
     return !this.audio.paused && this.audio.readyState >= 2;
   }

   return false;
 }

 isLoading(): boolean {
   if (this.isUsingNativeService) {
     return false; 
   }

   return this.audio ? this.audio.readyState < 2 : false;
 }

 getAudio(): HTMLAudioElement | null {
   return this.audio;
 }

 getConnectionStatus(): string {
   if (!this.audio && !this.isUsingNativeService) return 'disconnected';

   if (this.isUsingNativeService) return 'connected';

   if (this.audio) {
     if (this.audio.error) return 'error';
     if (this.audio.readyState === 0) return 'connecting';
     if (this.audio.readyState >= 2 && !this.audio.paused) return 'connected';
     if (this.audio.readyState >= 3) return 'connected';
   }

   return 'connecting';
 }

 isUsingNative(): boolean {
   return this.isUsingNativeService;
 }
}
