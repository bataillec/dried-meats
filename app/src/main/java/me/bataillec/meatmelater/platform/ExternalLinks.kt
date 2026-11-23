package me.bataillec.meatmelater.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

/** R29: the only two outside destinations the app links to. Opened by the phone's browser. */
object SupportLinks {
    const val KOFI_URL = "https://ko-fi.com/O0P52B4FJ1"
    const val GITHUB_URL = "https://github.com/bataillec/dried-meats"
}

/** Hands a URL to another app. Returns false when nothing can open it. */
fun interface ExternalLinkOpener {
    fun open(url: String): Boolean
}

/** R29, R20: `ACTION_VIEW` only. No permission, no WebView, the app fetches nothing itself. */
class AndroidExternalLinkOpener(
    private val context: Context,
) : ExternalLinkOpener {
    override fun open(url: String): Boolean =
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
}

@Composable
fun rememberExternalLinkOpener(): ExternalLinkOpener {
    val context = LocalContext.current
    return remember(context) { AndroidExternalLinkOpener(context) }
}
