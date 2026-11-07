# LoL SkinScope

App Android (Kotlin + Jetpack Compose) para consultar skins de League of Legends: catálogo,
skins que posees, última disponibilidad en tienda, predicciones por temática y estadísticas de
skin favorita por campeón.

## Estado

**Fase 1 completada** — catálogo local de campeones y skins con:

- Listado y búsqueda de campeones (filtros por nombre y alias, ignorando mayúsculas).
- Detalle de campeón: splash, título, tags localizados, lore completo y licencia de skins.
- Skins con precios (RP / Esencia Mítica), rareza, sinergias (skin lines) y marca de legado.
- **Reproductor embebido de SkinSpotlights**: cada skin abre un video de YouTube dentro de la app
  (resolución automática del videoId vía búsqueda en el canal + embed con referer propio).
  El modal persiste al rotar, va a pantalla completa en horizontal con barras inmersivas y
  soporta el botón de fullscreen del reproductor.
- **Idioma conmutable** (Español MX / English NA) desde la barra superior: re-sincroniza el
  catálogo y todo el contenido (nombres, títulos, lore, skin lines, precios) y la interfaz.
- Lore completo precargado en segundo plano tras cada sincronización (Data Dragon por campeón).
- Catálogo sincronizado en Room (esquema v4) con versionado por parche, esquema e idioma.

## Stack

- Kotlin 2.3.20, AGP 9.0.1, Gradle 9.1.0, JDK 17 (toolchain)
- Jetpack Compose (BOM 2026.03.01) + Material 3, Navigation 3
- Hilt + KSP para inyección de dependencias
- Retrofit 3 + OkHttp 5 + kotlinx.serialization
- Room, DataStore, Paging 3, Coil 3
- WebView (overlay propio, sin ventana de diálogo) para el embed de YouTube

## Requisitos

- Android Studio 2026.2.2 o superior (JBR 25)
- SDK de Android 37, minSdk 26
- Emulador o dispositivo con Android 8.0+

## Compilar y ejecutar

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug testDebugUnitTest
```

Instalar en un dispositivo conectado:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Estructura

```
app/src/main/kotlin/com/thikeiw/lolinfo/
├── LoLSkinScopeApp.kt            # Application + @HiltAndroidApp
├── MainActivity.kt               # Entry point Compose + idioma (attachBaseContext)
├── Navigation.kt                 # Navigation 3 (NavDisplay)
├── data/
│   ├── CatalogRepository.kt      # Sincronización (Data Dragon + CommunityDragon) y flows
│   ├── local/                    # Room (lolskin.db), DataStore (versión/esquema/idioma), precios
│   ├── mappers/                  # Entidades ↔ dominio; tags y rarezas crudas -> recursos
│   └── remote/                   # Retrofit: DataDragonService, CommunityDragonService
├── di/                           # Módulos Hilt (red, room, repositorios)
├── domain/model/                 # Modelos de dominio (Champion, Skin)
├── ui/
│   ├── champions/                # Listado + búsqueda + selector de idioma
│   ├── championdetail/           # Detalle + skins + video
│   ├── skinvideo/                # Overlay de video (WebView oculto -> embed, fullscreen)
│   └── theme/                    # Tema hextech
├── util/                         # SpotlightUrls (búsqueda/1305 + fallback)
└── res/
    ├── values/                   # Español (MX)
    └── values-en/                # English (NA)
```

## Fuentes de datos

- [Data Dragon](https://developer.riotgames.com/docs/lol#data-dragon): campeones, lore, skins y
  assets (locale por idioma: `es_MX` / `en_US`).
- [CommunityDragon](https://www.communitydragon.org/): metadatos extra (rarity cruda = código `k*`,
  `isLegacy`, `skinLines`, precios). Nota: el inglés vive en la carpeta `default` (no `en_us`).
- SkinSpotlights: videos embebidos vía búsqueda del canal (`youtube.com/@SkinSpotlights/search`),
  con cookies de consentimiento, user-agent desktop y el nombre del skin SIEMPRE en inglés, que
  es como el canal titula sus videos.
- La API oficial de Riot **no expone** las skins que posee un jugador: se marcarán manualmente.
- El historial de disponibilidad no tiene API oficial: se estimará con snapshots periódicos.

## Detalle técnico del idioma

Los datos guardados en Room vienen con el idioma elegido; como Data Dragon/CommunityDragon
entregan tags (`Mage`) y rarezas (`kEpic`) solo en inglés, estos se almacenan crudos y la UI los
traduce por recursos (`tag_mage`, `skin_rarity_*`), cambiando al instante con el idioma. La
interfaz de la app usa `values/` (español MX) y `values-en/`; el idioma se aplica en
`attachBaseContext` y se recrea la Activity al cambiarlo.

## Roadmap

1. ✅ Fase 0: entorno, proyecto, build y conexión a Data Dragon
2. ✅ Fase 1: catálogo local (Room) + listado y detalle de campeones/skins + video in-app + idioma
3. Fase 2: colección personal, filtros y búsqueda avanzada
4. Fase 3: Firebase (auth anónima/Google, Firestore) y sincronización
5. Fase 4: historial de tienda y predicciones por temática
6. Fase 5: estadísticas y gráficas (Vico)
7. Fase 6: pulido, accesibilidad y tests
