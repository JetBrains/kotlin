// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintServiceCastInspection

import android.content.ClipboardManager
import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.service.wallpaper.WallpaperService

@Suppress("UsePropertyAccessSyntax", "UNUSED_VARIABLE", "unused", "UNUSED_PARAMETER", "DEPRECATION")
class SystemServiceTest : Activity() {

    fun test1() {
        val displayServiceOk = getSystemService(DISPLAY_SERVICE) as DisplayManager
        val displayServiceWrong = <error descr="Suspicious cast to `DisplayManager` for a `DEVICE_POLICY_SERVICE`: expected `DevicePolicyManager`">getSystemService(DEVICE_POLICY_SERVICE) as DisplayManager</error>
        val wallPaperOk = getSystemService(WALLPAPER_SERVICE) as WallpaperService
        val wallPaperWrong = <error descr="Suspicious cast to `WallpaperManager` for a `WALLPAPER_SERVICE`: expected `WallpaperService`">getSystemService(WALLPAPER_SERVICE) as WallpaperManager</error>
    }

    fun test2(context: Context) {
        val displayServiceOk = context.getSystemService(DISPLAY_SERVICE) as DisplayManager
        val displayServiceWrong = <error descr="Suspicious cast to `DisplayManager` for a `DEVICE_POLICY_SERVICE`: expected `DevicePolicyManager`">context.getSystemService(DEVICE_POLICY_SERVICE) as DisplayManager</error>
    }

    fun clipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipboard2 = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    }
}