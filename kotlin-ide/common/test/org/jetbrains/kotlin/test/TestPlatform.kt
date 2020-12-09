package org.jetbrains.kotlin.test

object TestPlatform {
    private const val KEY = "kombo.android.studio.mode"

    fun checkIsAndroidStudio(): Boolean {
        return when (val value = System.getProperty(KEY, null)) {
            "ci" -> true
            "local" -> throw AssertionError("Test is not expected to be run under Android Studio. It will be ignored on CI.")
            null -> false
            else -> throw AssertionError("Unexpected value for key $KEY: $value")
        }
    }
}