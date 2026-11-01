# LoL SkinScope

App Android (Kotlin + Jetpack Compose) para consultar skins de League of Legends: catálogo,
skins que posees, última disponibilidad en tienda, predicciones por temática y estadísticas de
skin favorita por campeón.

## Estado

**Fase 0 — esqueleto técnico.** La pantalla principal valida la cadena completa
Retrofit → Data Dragon → Hilt → ViewModel → Compose mostrando las versiones publicadas del
parche.

## Stack

- Kotlin 2.3.20, AGP 9.0.1, Gradle 9.1.0, JDK 17 (toolchain)
- Jetpack Compose (BOM 2026.03.01) + Material 3, Navigation 3
- Hilt + KSP para inyección de dependencias
- Retrofit 3 + OkHttp 5 + kotlinx.serialization
- Room, DataStore, Paging 3, Coil 3

## Requisitos

- Android Studio 2026.2.2 o superior (JBR 25)
- SDK de Android 37, minSdk 26
- Emulador o dispositivo con Android 8.0+

## Compilar y ejecutar

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```

Instalar en un emulador activo:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Estructura

```
app/src/main/kotlin/com/thikeiw/lolinfo/
├── LoLSkinScopeApp.kt      # Application + @HiltAndroidApp
├── MainActivity.kt         # Entry point Compose
├── Navigation.kt           # Navigation 3 (NavDisplay)
├── data/                   # Repositorios y fuentes remotas
├── di/                     # Módulos Hilt
└── ui/                     # Pantallas y tema (paleta hextech)
```

## Fuentes de datos

- [Data Dragon](https://developer.riotgames.com/docs/lol#data-dragon): catálogo de campeones,
  skins y assets.
- [CommunityDragon](https://www.communitydragon.org/): metadatos extra (`rarity`, `isLegacy`,
  `skinLines`).
- La API oficial de Riot **no expone** las skins que posee un jugador: se marcan manualmente.
- El historial de disponibilidad no tiene API oficial: se estima con snapshots periódicos.

## Roadmap

1. Fase 0: entorno, proyecto, build y conexión a Data Dragon ✅
2. Fase 1: catálogo local (Room) + listado y detalle de campeones/skins
3. Fase 2: colección personal, filtros y búsqueda
4. Fase 3: Firebase (auth anónima/Google, Firestore) y sincronización
5. Fase 4: historial de tienda y predicciones por temática
6. Fase 5: estadísticas y gráficas (Vico)
7. Fase 6: pulido, accesibilidad y tests
