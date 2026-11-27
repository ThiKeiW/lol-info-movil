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
- Catálogo sincronizado en Room (esquema v5) con versionado por parche, esquema e idioma.

**Fase 2 completada** — colección personal y filtros:

- **Skin favorita por campeón**: icono de corazón en cada card de skin (dorada si es favorita).
- **Skins en posesión**: texto "En posesión" (verde) / "No obtenido" (gris) junto al botón de
  reproducir video; tocar alterna la marca.
- Las marcas viven en la tabla Room `skin_flags` (DB v5), separadas del catálogo para no perderse
  al re-sincronizar.
- **Vista de skins favoritas** y **vista de skins en posesión** (íconos corazón/inventario en la
  barra de Campeones), reutilizando el formato de cards y el reproductor de video.
- **Búsqueda y filtros de skins** en ambas vistas: texto (skin o campeón), chips de rareza
  (épica, legendaria, mítica, etc.), legado y limpiar filtros.
- Card de skin unificada en `ui/skins/SkinCard.kt`, compartida por detalle y colecciones. El título
  muestra solo el nombre de la skin (p. ej. `Aatrox Justiciero`); el nombre del campeón ya no se
  repite debajo, aunque sigue disponible para búsqueda y orden en las vistas de colección.
- **Estado actual:** build y tests unitarios en verde (`assembleDebug testDebugUnitTest`).

**Fase 3 completada** — cuentas y sincronización:

- **Auth**: sesión anónima automática y opción de vincular Google con Credential Manager conservando
  el mismo `uid`; cerrar sesión no borra la caché local.
- **Marcas por cuenta**: `skin_flags` usa PK `(uid, skinId)`; cada cuenta tiene su propia colección
  y la UI reacciona al cambiar de sesión. Las marcas previas se reclaman para la primera cuenta
  (`uid = "legacy"`).
- **Sincronización con Firestore** (`users/{uid}/skinFlags/{skinId}`) con persistencia offline: el
  merge usa un reloj lógico (Lamport `revision` + `updatedBy`), sin depender del reloj del equipo.
- **Pantalla «Cuenta y sincronización»** (icono de perfil en Campeones): estado de sesión, vincular
  Google, cerrar sesión, sincronizar ahora y último sync.
- Estado: `assembleDebug testDebugUnitTest` en verde y verificado en dispositivo físico
  (sesión anónima, vinculación con Google y sincronización).

**Audios de voz** - reproducción por skin y voz base:

- Botón de audio en cada SkinCard y acción de voz base en la cabecera del detalle.
- VO por skin/base desde [lolsound.com](https://lolsound.com/) (tercero no oficial), en inglés o español latino según idioma de la app.
- Si una skin no tiene voces propias, se muestra estado vacío; los audios que fallen al reproducir se omiten automáticamente.
- Reproductor en bottom sheet (MediaPlayer streaming) con play/pausa, anterior/siguiente, seek, secciones y atribución.

## Fuentes de datos

- [lolsound.com](https://lolsound.com/): voces de campeones/skins (tercero no oficial). Audio © Riot Games.

- La API oficial de Riot **no expone** las skins que posee un jugador: se marcan manualmente
  (Fase 2) y se guardan localmente en Room (`skin_flags`).
- Cloud Firestore: marcas por usuario en `users/{uid}/skinFlags`, con reglas en `firestore.rules`
  y caché offline por defecto. La fusión usa reloj lógico (Lamport), no la hora del dispositivo.
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
3. ✅ Fase 2: colección personal (favoritas/en posesión) + filtros y búsqueda de skins
4. ✅ Fase 3: Firebase (auth anónima/Google, Firestore), marcas por cuenta y sincronización
5. Fase 4: historial de tienda y predicciones por temática
6. Fase 5: estadísticas y gráficas (Vico)
7. Fase 6: pulido, accesibilidad y tests
