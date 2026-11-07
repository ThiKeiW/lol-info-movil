package com.thikeiw.lolinfo.ui.skinvideo

import androidx.activity.compose.BackHandler
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.thikeiw.lolinfo.R
import com.thikeiw.lolinfo.util.SpotlightUrls
import com.thikeiw.lolinfo.util.embedVideoUrl
import com.thikeiw.lolinfo.util.spotlightQuery
import kotlinx.coroutines.delay

/**
 * Modal que reproduce el spotlight del skin dentro de la app, sin salir de ella: un WebView oculto
 * abre la búsqueda del canal de SkinSpotlights en modo escritorio, extrae el videoId del primer
 * resultado y el WebView carga el reproductor embed de YouTube de ese video.
 *
 * En vertical: modal centrado con encabezado y video 16:9. En horizontal (o al pulsar el botón
 * pantalla completa del reproductor): video a pantalla completa sobre fondo negro.
 */
private const val RESOLVE_TIMEOUT_MS = 15_000L

private const val CONSENT_COOKIES = "CONSENT=YES+cb.20210328-17-p0.en+FX+626"

/** Flags inmersivos legacy: ocultan status bar + navegación (método fiable para dialogs). */
private val IMMERSIVE_FLAGS =
  View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
    View.SYSTEM_UI_FLAG_FULLSCREEN or
    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

private const val FIND_VIDEO_JS =
  "(function(){var m=document.documentElement.innerHTML.match(/" +
    "\"videoRenderer\":\\{\"videoId\":\"([A-Za-z0-9_-]{11})\"/);return m?m[1]:'';})()"

@Suppress("DEPRECATION")
@Composable
fun SkinVideoOverlay(
  championName: String,
  skinName: String,
  championId: String,
  skinNum: Int,
  englishSkinNameResolver: suspend () -> String?,
  onDismiss: () -> Unit,
) {
  // La búsqueda en YouTube se hace SIEMPRE con el nombre en inglés del skin (menos errores al
  // buscar en el canal SkinSpotlights); fallback: los nombres localizados visibles.
  var query by remember { mutableStateOf<String?>(null) }
  var videoId by remember { mutableStateOf<String?>(null) }
  var failed by remember { mutableStateOf(false) }
  var isFullscreen by remember { mutableStateOf(false) }

  LaunchedEffect(championId, skinNum) {
    query =
      when (val english = englishSkinNameResolver()) {
        null -> spotlightQuery(championName, skinName)
        else -> SpotlightUrls.queryOf(championId, english)
      }
  }

  val configuration = LocalConfiguration.current
  val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
  // Overlay dentro de la ventana de la Activity sin ventana de dialog intermedia: así el
  // inmersivo (ocultar status bar + navegación) se controla de forma fiable con los controllers
  // y flags de la propia ventana.
  val activity = LocalContext.current.findActivity()
  val insetsController =
    remember(activity) {
      activity?.window?.let { window -> WindowCompat.getInsetsController(window, window.decorView) }
    }

  LaunchedEffect(videoId) {
    if (videoId == null) {
      delay(RESOLVE_TIMEOUT_MS)
      if (videoId == null) failed = true
    }
  }

  // Fullscreen/horizontal: barras inmersivas (se ocultan status + navegación) + bloqueo
  // horizontal; al salir (o cerrar el video) se libera todo.
  DisposableEffect(insetsController, isFullscreen, isLandscape) {
    val immersive = isFullscreen || isLandscape
    if (immersive) {
      if (isFullscreen) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
      }
      activity?.window?.decorView?.systemUiVisibility = IMMERSIVE_FLAGS
      insetsController?.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      insetsController?.hide(WindowInsetsCompat.Type.systemBars())
    } else {
      activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
      insetsController?.show(WindowInsetsCompat.Type.systemBars())
    }
    onDispose {
      activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
      activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
      insetsController?.show(WindowInsetsCompat.Type.systemBars())
    }
  }

  // Back presiona: salir de fullscreen si está activo, sino cerrar el video.
  BackHandler {
    if (isFullscreen) isFullscreen = false else onDismiss()
  }

  Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
    Surface(
        shape =
          if (isLandscape) {
            RoundedCornerShape(0.dp)
          } else {
            RoundedCornerShape(20.dp)
          },
        modifier =
          if (isFullscreen || isLandscape) {
            Modifier.fillMaxSize()
          } else {
            Modifier.fillMaxWidth().padding(16.dp)
          },
        color = if (isFullscreen || isLandscape) Color.Black else MaterialTheme.colorScheme.surface,
      ) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          // En horizontal/fullscreen el video ocupa toda la pantalla (sin encabezado).
          if (isFullscreen || isLandscape) {
            VideoBox(
              videoId = videoId,
              failed = failed,
              query = query,
              onVideoResolved = {
                videoId = it
              },
              onFailed = { failed = true },
              onFullscreenChanged = { isFullscreen = it },
              showCloseButton = true,
              modifier = Modifier.fillMaxSize(),
              onDismiss = onDismiss,
              isFullscreen = isFullscreen,
              setFullscreen = { isFullscreen = it },
            )
          } else {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = skinName,
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text = stringResource(R.string.skin_video_channel),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              IconButton(onClick = onDismiss) {
                Icon(
                  imageVector = Icons.Filled.Close,
                  contentDescription = stringResource(R.string.action_close),
                )
              }
            }

            VideoBox(
              videoId = videoId,
              failed = failed,
              query = query,
              onVideoResolved = { videoId = it },
              onFailed = { failed = true },
              onFullscreenChanged = { isFullscreen = it },
              showCloseButton = false,
              modifier =
                Modifier
                  .fillMaxWidth()
                  .aspectRatio(16f / 9f)
                  .clip(RoundedCornerShape(12.dp)),
              onDismiss = onDismiss,
              isFullscreen = isFullscreen,
              setFullscreen = { isFullscreen = it },
            )
          }
        }
      }
    }
}

@Composable
private fun VideoBox(
  videoId: String?,
  failed: Boolean,
  query: String?,
  onVideoResolved: (String?) -> Unit,
  onFailed: () -> Unit,
  onFullscreenChanged: (Boolean) -> Unit,
  showCloseButton: Boolean,
  modifier: Modifier = Modifier,
  onDismiss: () -> Unit,
  isFullscreen: Boolean,
  setFullscreen: (Boolean) -> Unit,
) {
  Box(
    modifier =
      modifier.background(
        if (showCloseButton) {
          Color.Black
        } else {
          MaterialTheme.colorScheme.surfaceVariant
        },
      ),
  ) {
    val currentVideoId = videoId
    when {
      currentVideoId != null ->
        VideoWebView(
          videoId = currentVideoId,
          onFullscreenChanged = onFullscreenChanged,
          modifier = Modifier.fillMaxSize(),
        )
      failed ->
        Text(
          text = stringResource(R.string.skin_video_not_found),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.align(Alignment.Center).padding(24.dp),
        )
      else -> {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        val currentQuery = query
        if (currentQuery != null) {
          HiddenSearchWebView(
            searchUrl = SpotlightUrls.searchUrl(currentQuery),
            onVideoFound = { id ->
              if (id.isNotBlank()) onVideoResolved(id) else onFailed()
            },
            onFailed = onFailed,
            modifier = Modifier.size(1.dp).alpha(0f),
          )
        }
      }
    }

    // En vertical la X es redundante: el encabezado del modal ya provee el cierre.
    if (showCloseButton) {
      IconButton(
        onClick = {
          if (isFullscreen) setFullscreen(false) else onDismiss()
        },
        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Close,
          contentDescription = stringResource(R.string.action_close),
          tint = Color.White,
        )
      }
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun VideoWebView(
  videoId: String,
  onFullscreenChanged: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val webView = remember(videoId) { context.createVideoWebView(videoId, onFullscreenChanged) }
  DisposableEffect(videoId) {
    onDispose {
      webView.stopLoading()
      webView.destroy()
    }
  }
  AndroidView(factory = { webView }, modifier = modifier)
}

/**
 * El embed se carga como página HTML con un iframe: YouTube exige identificación vía
 * HTTP Referer de la app (https://<package>) con Referrer-Policy strict-origin-when-cross-origin;
 * sin esto devuelve error 152/153 en el reproductor.
 */
private fun embedHtml(videoId: String): String =
  """<!DOCTYPE html><html><head>
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <meta name="referrer" content="strict-origin-when-cross-origin">
    <style>html,body{margin:0;padding:0;height:100%;background:#000;overflow:hidden}
    iframe{width:100%;height:100%;border:0}</style></head>
    <body><iframe src="${embedVideoUrl(videoId)}"
    referrerpolicy="strict-origin-when-cross-origin"
    allow="autoplay; encrypted-media; picture-in-picture; fullscreen" allowfullscreen></iframe>
    </body></html>"""

/** La política de embeds de YouTube solo valida con user-agent móvil de Chrome. */
private const val MOBILE_VIDEO_UA =
  "Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) " +
    "Chrome/121.0.0.0 Mobile Safari/537.36"

/**
 * El botón pantalla completa del reproductor no usa la vista nativa de YouTube (se desestima al
 * instante: producía pantalla negra con el audio siguiendo en el WebView); el fullscreen real lo
 * implementa el modal expandiendo su propia superficie de video.
 */
private class FullscreenHandler(
  private val onFullscreenChanged: (Boolean) -> Unit,
) : WebChromeClient() {
  override fun onShowCustomView(view: View, callback: CustomViewCallback) {
    callback.onCustomViewHidden()
    onFullscreenChanged(true)
  }

  override fun onHideCustomView() {
    onFullscreenChanged(false)
  }
}

private fun Context.createVideoWebView(
  videoId: String,
  onFullscreenChanged: (Boolean) -> Unit,
): WebView =
  WebView(this).apply {
    layoutParams =
      ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
      )
    configurePlaybackSettings()
    settings.userAgentString = MOBILE_VIDEO_UA
    settings.allowFileAccess = false
    webChromeClient = FullscreenHandler(onFullscreenChanged)
    webViewClient =
      object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
          view: WebView,
          request: WebResourceRequest,
        ): Boolean = !request.url.toString().startsWith("http")
      }
    loadDataWithBaseURL(
      "https://$packageName",
      embedHtml(videoId),
      "text/html",
      "UTF-8",
      null,
    )
  }

/** WebView oculto (1px, invisible) que solo sirve para resolver el videoId de la búsqueda. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun HiddenSearchWebView(
  searchUrl: String,
  onVideoFound: (String) -> Unit,
  onFailed: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val webView =
    remember(searchUrl) {
      context.createResolvingWebView(searchUrl, onVideoFound = onVideoFound, onFailed = onFailed)
    }
  DisposableEffect(webView) {
    onDispose {
      webView.stopLoading()
      webView.destroy()
    }
  }
  AndroidView(
    factory = { webView },
    modifier = modifier,
    update = { view -> if (view.url != searchUrl) view.loadUrl(searchUrl) },
  )
}

private fun Context.createResolvingWebView(
  searchUrl: String,
  onVideoFound: (String) -> Unit,
  onFailed: () -> Unit,
): WebView =
  WebView(this).apply {
    layoutParams = ViewGroup.LayoutParams(1, 1)
    configurePlaybackSettings()
    // Cookies de consentimiento de YouTube para que la búsqueda devuelta incluya resultados.
    CookieManager.getInstance().apply {
      setAcceptCookie(true)
      setCookie("https://www.youtube.com", CONSENT_COOKIES)
      setCookie("https://www.youtube.com", "SOCS=CAI")
      flush()
    }
    webChromeClient = WebChromeClient()
    webViewClient =
      object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
          view: WebView,
          request: WebResourceRequest,
        ): Boolean = !request.url.toString().startsWith("http")

        override fun onPageFinished(view: WebView, url: String) {
          if (!url.startsWith("https://www.youtube.com/@")) return
          view.evaluateJavascript(FIND_VIDEO_JS) { result ->
            val id = result.removeSurrounding("\"")
            if (id.isNotBlank() && id.length == 11) onVideoFound(id) else onFailed()
          }
        }
      }
    loadUrl(searchUrl)
  }

private fun WebView.configurePlaybackSettings() {
  settings.javaScriptEnabled = true
  settings.domStorageEnabled = true
  settings.mediaPlaybackRequiresUserGesture = false
  settings.userAgentString = SpotlightUrls.DESKTOP_USER_AGENT
  settings.useWideViewPort = true
  settings.loadWithOverviewMode = true
}

private tailrec fun Context.findActivity(): Activity? =
  when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
  }
