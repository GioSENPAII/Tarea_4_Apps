# MapasNativosApp

## Descripción del Proyecto

MapasNativosApp es una aplicación Android que permite comparar y utilizar diferentes servicios de mapas web (OpenStreetMap y Google Maps) en un entorno nativo de Android. La aplicación está diseñada para demostrar la integración de mapas web en aplicaciones Android mediante WebView, comparar el rendimiento y la funcionalidad de diferentes proveedores de mapas, y ofrecer características adicionales como exploración urbana y generación de rutas.

### Características Principales

- **Comparación de Mapas**: Visualiza y compara OpenStreetMap y Google Maps en pestañas separadas.
- **Exploración Urbana**: Descubre zonas predefinidas en el mapa, con un sistema de progreso para gamificar la experiencia.
- **Puntos de Interés**: Guarda y gestiona tus ubicaciones favoritas o puntos de interés.
- **Generación de Rutas**: Crea rutas entre puntos seleccionados con diferentes modos de transporte (a pie, bicicleta, auto).
- **Rendimiento**: Comparativa de tiempos de carga entre diferentes proveedores de mapas.

## Instrucciones de Configuración

### Requisitos Previos

- Android Studio Koala (2023.1.0) o superior
- JDK 11 o superior
- Un dispositivo Android con API 24 (Android 7.0) o superior, o un emulador

### Configuración del Entorno

1. Clona el repositorio: 
git clone https://github.com/tuusuario/MapasNativosApp.git

2. Abre el proyecto en Android Studio:
- Inicia Android Studio
- Selecciona "Open an Existing Project"
- Navega hasta la carpeta del proyecto clonado y selecciónala

3. Sincroniza el proyecto con Gradle:
- Android Studio debería sincronizar automáticamente
- Si no lo hace, selecciona "File > Sync Project with Gradle Files"

4. Configura un dispositivo o emulador:
- Conecta un dispositivo Android físico mediante USB con la depuración USB habilitada, o
- Configura un emulador desde el AVD Manager en Android Studio

### Ejecución de la Aplicación

1. Una vez configurado el entorno, haz clic en el botón "Run" (▶️) en Android Studio
2. Selecciona el dispositivo o emulador donde deseas ejecutar la aplicación
3. Espera a que la aplicación se compile e instale en el dispositivo
4. La aplicación debería iniciarse automáticamente

## Arquitectura de la Aplicación

La aplicación sigue una arquitectura MVVM (Model-View-ViewModel) con los siguientes componentes:

### Capas de la Aplicación

1. **Capa de Presentación**
- Activities y Fragments (View)
- ViewModels
- Adaptadores

2. **Capa de Dominio/Negocio**
- Repositorios
- Sistemas especializados (ExplorationSystem)

3. **Capa de Datos**
- Room Database
- DAOs (Data Access Objects)
- Modelos de datos

### Diagrama Conceptual

┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│                  │     │                  │     │                  │
│  Presentación    │     │     Dominio      │     │      Datos       │
│  (UI/Activities) │◄───►│  (Repositorios)  │◄───►│   (Room/DAOs)    │
│                  │     │                  │     │                  │
└──────────────────┘     └──────────────────┘     └──────────────────┘

### Componentes Principales

- **WebView**: Para mostrar y gestionar los mapas web
- **Room Database**: Para almacenamiento persistente de puntos de interés
- **LiveData/ViewModel**: Para comunicación reactiva entre UI y datos
- **ViewPager2**: Para navegación entre pestañas de mapas
- **FusedLocationProviderClient**: Para obtener la ubicación actual del usuario

## Desafíos y Soluciones

### Desafío 1: Integración de Mapas Web en WebView

**Problema**: Los mapas web como Google Maps y OpenStreetMap tienen diferentes requisitos y comportamientos dentro de WebView.

**Solución**: Se implementó una configuración personalizada para cada proveedor de mapas, ajustando opciones como User-Agent, JavaScript, y manejo de permisos. Para Google Maps, se utilizó una implementación basada en la API JavaScript de Google Maps para evitar redirecciones.

### Desafío 2: Gestión de Permisos de Ubicación

**Problema**: Los permisos de ubicación en Android son críticos y deben manejarse adecuadamente.

**Solución**: Se implementó un sistema robusto de solicitud de permisos utilizando ActivityResultContracts, con lógica de fallback para proporcionar una ubicación predeterminada cuando no hay permisos disponibles.

### Desafío 3: Comunicación entre JavaScript y Kotlin

**Problema**: La comunicación bidireccional entre el código Kotlin de la aplicación y el JavaScript en WebView.

**Solución**: Se utilizó JavascriptInterface para exponer métodos de Kotlin a JavaScript, y evaluateJavascript para llamar a funciones JavaScript desde Kotlin, permitiendo una interacción fluida entre ambos entornos.

### Desafío 4: Rendimiento de WebView

**Problema**: El rendimiento de WebView puede variar significativamente según el contenido y la configuración.

**Solución**: Se implementó un sistema de medición de tiempos de carga y se optimizaron las configuraciones de WebView (caché, almacenamiento DOM, opciones de renderizado) para mejorar el rendimiento.

## Dependencias Utilizadas

### Dependencias Principales

- **AndroidX Core KTX (1.15.0)**: Extensiones de Kotlin para Android
- **AppCompat (1.7.0)**: Compatibilidad con versiones anteriores de Android
- **Material Design (1.12.0)**: Componentes de UI siguiendo las guías de Material Design
- **ConstraintLayout (2.2.1)**: Layout flexible para diseños complejos
- **ViewPager2 (1.0.0)**: Para navegación con pestañas
- **Google Play Services Location (21.0.1)**: API de localización de Google

### Dependencias de Base de Datos

- **Room (2.6.0)**: ORM para SQLite con soporte para operaciones asíncronas
  - room-runtime: Base de la biblioteca Room
  - room-ktx: Extensiones de Kotlin para Room
  - room-compiler: Procesador de anotaciones para Room

### Dependencias de Concurrencia

- **Kotlin Coroutines (1.7.3)**: Para programación asíncrona
  - kotlinx-coroutines-core: Núcleo de corrutinas
  - kotlinx-coroutines-android: Integraciones de Android para corrutinas

### Dependencias de Arquitectura

- **Lifecycle (2.6.2)**: Componentes para gestión del ciclo de vida
  - lifecycle-viewmodel-ktx: ViewModel con soporte para corrutinas
  - lifecycle-livedata-ktx: LiveData con soporte para corrutinas

### Otras Dependencias

- **Gson (2.10.1)**: Para serialización/deserialización de JSON

## Próximas Mejoras

- Implementación de un modo offline con almacenamiento en caché de mapas
- Soporte para navegación paso a paso con instrucciones de voz
- Integración con APIs externas para obtener información de POIs
- Mejora del sistema de gamificación con logros y recompensas
- Optimización del rendimiento en dispositivos de gama baja
