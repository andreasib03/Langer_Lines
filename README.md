# Linee Langer - Clinical Skin Analysis

**Linee Langer** è un'applicazione Android avanzata dedicata all'analisi dermatologica ed estetica.

Utilizza algoritmi di **Computer Vision** per identificare e visualizzare l'orientamento delle linee di tensione cutanea (Linee di Langer) in tempo reale

## 🚀 Funzionalità Principali

- **Analisi Live**: Rilevamento automatico delle linee tramite fotocamera (CameraX + OpenCV)
- **Analisi Galleria**: Caricamento e processamento di immagini esistenti con strumenti di trasformazione
- **Dermatologia Digitale**: Supporto specifico per diverse aree del corpo (viso, braccia, gambe, ecc) con parametri di sensibilità ottimizzati
- **Storico Analisi**: Persistenza locale delle analisi effettuate con dettagli tecnici e visualizzazione grafica
- **Cloud Sync**: Sincronizzazione sicura dei dati tra dispositivi tramite Firebase
- **Smart Notifications**: Promemoria per il monitoraggio costante e notifiche di completamento processi

## 🛠 Stack Tecnologico

- **Linguaggio**: Kotlin
- **UI**: Jetpack Compose (Modern Toolkit)
- **Architettura**: MVVM + Clean Architecture (Data, Domain, UI)
- **Computer Vision**: OpenCV (Native C++ integration via JNI)
- **Dependency Injection**: Hilt (Dagger)
- **Database**: Room (Persistence) + DataStore (Preferences)
- **Networking**: Firebase (Auth, Firestore)
- **Background Tasks**: WorkManager (Sync & Cleanup)
- **Media**: CameraX + Image Processing API

## 📐 Algoritmo di Rilevamento

Il core dell'app risiede nel `LangerDetector`, che implementa:

1. **Pre-processing**: Filtro Bilaterale (denoising) e CLAHE (contrasto adattivo)
2. **Structure Tensor**: Calcolo dei gradienti tramite operatori di Scharr
3. **Coerenza di Weickert**: Analisi della direzione preferenziale dei tessuti
4. **Anatomical Priors**: Integrazione di conoscenza medica per migliorare la precisione del tracciamento

## 📦 Struttura del Progetto

- `app/src/main/java/com/example/linee_langer/`
  - `core/`: Configurazione DI, Database e Utility
  - `data/`: Implementazione dei Repository e Data Sources (Local/Remote)
  - `domain/`: Business logic, Modelli e Use Cases
  - `ui/`: Schermate Compose, Navigation e Temi
  - `worker/`: Task in background per sincronizzazione e manutenzione
