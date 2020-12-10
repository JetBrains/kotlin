package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.utils.Printer
import java.io.File

object AndroidStudioTestUtils {
    private const val KEY = "kombo.android.studio.mode"

    /**
     * Checks if the test is run against Android Studio.
     *
     * Kotlin IDE plugin tests can run against IDEA sources and Android Studio binaries.
     * It doesn't make sense to run some tests in Android Studio as they depend on components absent in it (JPS, Maven etc.)
     *
     * This method is supposed to be called from UsefulTestCase.shouldRunTest(). It silently returns 'false' on CI, however
     * it fails with an easily recognizable exception locally, so the developer would understand what's going on.
     */
    fun checkIsAndroidStudio(): Boolean {
        return when (val value = System.getProperty(KEY, null)) {
            "ci" -> true
            "local" -> throw AssertionError("Test is not expected to be run under Android Studio. It will be ignored on CI.")
            null -> false
            else -> throw AssertionError("Unexpected value for key $KEY: $value")
        }
    }

    /**
     * Adds explicit 'android.dir' preference to 'local.properties' of a Gradle project, disabling Android SDK
     * discovery in 'com.android.tools.idea.gradle.project.sync.SdkSync.syncIdeAndProjectAndroidSdk'.
     */
    fun specifyAndroidSdk(projectRoot: File) {
        if (System.getProperty(KEY, null) != null) {
            val androidSdkDir = File(projectRoot, "androidSdk")

            // Android plugin doesn't recognize an Android platform if there's no platforms dir
            // See 'com.android.tools.idea.sdk.SdkPaths.validateAndroidSdk' for more information
            File(androidSdkDir, "platforms").mkdirs()

            File(projectRoot, "local.properties").appendText(Printer.LINE_SEPARATOR + "android.dir=${androidSdkDir.path}")
        }
    }
}