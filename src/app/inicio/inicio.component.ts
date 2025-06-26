import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AudioService } from '../services/audio.service';

@Component({
 selector: 'app-inicio',
 standalone: true,
 imports: [CommonModule],
 templateUrl: './inicio.component.html',
 styleUrls: ['./inicio.component.css']
})
export class InicioComponent implements OnInit, OnDestroy {
 isLoading = true;  // ← CAMBIO: Empieza en true para mostrar loading inmediatamente
 isPlaying = false;
 private audioCheckInterval: any;
 private loadingTimeout: any;

 constructor(private audioService: AudioService) {}

 ngOnInit() {
   // AUTO-INICIAR la transmisión apenas entre a la app
   console.log('App iniciada - mostrando loading y iniciando transmisión');

   // Iniciar INMEDIATAMENTE sin delay
   this.autoStartRadio();
 }

 ngOnDestroy() {
   // Limpiar intervalos al destruir el componente
   this.clearIntervals();
 }

 private clearIntervals() {
   if (this.audioCheckInterval) {
     clearInterval(this.audioCheckInterval);
     this.audioCheckInterval = null;
   }
   if (this.loadingTimeout) {
     clearTimeout(this.loadingTimeout);
     this.loadingTimeout = null;
   }
 }

 private autoStartRadio() {
   console.log('Iniciando transmisión automáticamente...');

   // Iniciar la radio
   this.audioService.playRadio();

   // Empezar a verificar INMEDIATAMENTE si está sonando
   this.startAudioDetection();

   // Timeout de seguridad - máximo 10 segundos
   this.loadingTimeout = setTimeout(() => {
     if (this.isLoading) {
       console.log('Timeout de carga - asumiendo que está funcionando');
       this.isLoading = false;
       this.isPlaying = true;
       this.clearIntervals();
     }
   }, 10000);
 }

 private startAudioDetection() {
   // Verificar cada 100ms si ya está sonando
   this.audioCheckInterval = setInterval(() => {
     this.checkIfAudioIsPlaying();
   }, 100);
 }

 private async checkIfAudioIsPlaying() {
   try {
     // Verificar usando el servicio de audio
     const isCurrentlyPlaying = await this.audioService.isPlaying();

     // Verificar también el estado de conexión
     const connectionStatus = this.audioService.getConnectionStatus();

     // Si detectamos que está reproduciendo O está conectado
     if (isCurrentlyPlaying || connectionStatus === 'connected') {
       console.log('¡Audio detectado! Quitando loading...');
       this.isLoading = false;
       this.isPlaying = true;
       this.clearIntervals();
       return;
     }

     // Verificar también el audio HTML si está disponible
     const htmlAudio = this.audioService.getAudio();
     if (htmlAudio && !htmlAudio.paused && htmlAudio.readyState >= 3) {
       console.log('¡Audio HTML detectado! Quitando loading...');
       this.isLoading = false;
       this.isPlaying = true;
       this.clearIntervals();
       return;
     }

   } catch (error) {
     console.error('Error verificando audio:', error);
   }
 }

 play() {
   if (this.isPlaying || this.isLoading) return;

   this.isLoading = true;
   this.isPlaying = false;

   console.log('Iniciando transmisión manualmente...');

   // Iniciar la radio
   this.audioService.playRadio();

   // Empezar detección inmediata
   this.startAudioDetection();

   // Timeout de seguridad
   this.loadingTimeout = setTimeout(() => {
     if (this.isLoading) {
       console.log('Timeout manual - asumiendo que está funcionando');
       this.isLoading = false;
       this.isPlaying = true;
       this.clearIntervals();
     }
   }, 8000);
 }

 stop() {
   // Limpiar todo inmediatamente
   this.clearIntervals();

   // Detener el servicio
   this.audioService.stopRadio();

   // Actualizar estado inmediatamente
   this.isPlaying = false;
   this.isLoading = false;

   console.log('Transmisión detenida');
 }
}
