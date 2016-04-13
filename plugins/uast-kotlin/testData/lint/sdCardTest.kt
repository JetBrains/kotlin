// INSPECTION_CLASS: org.jetbrains.android.inspections.klint.AndroidLintInspectionToolProvider$AndroidKLintSdCardPathInspection

import java.io.File
import android.content.Intent
import android.net.Uri

/**
 * Ignore comments - create("/sdcard/foo")
 */
@Suppress("UsePropertyAccessSyntax", "UNUSED_VARIABLE", "unused", "UNUSED_PARAMETER", "DEPRECATION")
class SdCardTest {
    internal var deviceDir = File("<warning>/sdcard/vr</warning>")

    init {
        if (PROFILE_STARTUP) {
            android.os.Debug.startMethodTracing("<warning><warning>/sdcard/launcher</warning></warning>")
        }

        if (File("<warning><warning>/sdcard</warning></warning>").exists()) {
    }
    val FilePath = "<warning><warning>/sdcard/</warning></warning>" + File("test")
    System.setProperty("foo.bar", "file://sdcard")


    val intent = Intent(Intent.ACTION_PICK)
    intent.setDataAndType(Uri.parse("<warning><warning>file://sdcard/foo.json</warning></warning>"), "application/bar-json")
    intent.putExtra("path-filter", "<warning><warning>/sdcard(/.+)*</warning></warning>")
    intent.putExtra("start-dir", "<warning><warning>/sdcard</warning></warning>")
    val mypath = "<warning><warning>/data/data/foo</warning></warning>"
    val base = "<warning><warning>/data/data/foo.bar/test-profiling</warning></warning>"
    val s = "<warning><warning>file://sdcard/foo</warning></warning>"
}

companion object {
    private val PROFILE_STARTUP = true
    private val SDCARD_TEST_HTML = "<warning>/sdcard/test.html</warning>"
    val SDCARD_ROOT = "<warning>/sdcard</warning>"
    val PACKAGES_PATH = "<warning>/sdcard/o/packages/</warning>"
}
}
