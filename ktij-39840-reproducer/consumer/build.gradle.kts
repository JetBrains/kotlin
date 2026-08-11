plugins {
    id("com.android.library") version "9.1.1" apply false
    // AGP 9.x ships built-in Kotlin for Android modules — do not apply kotlin-android.
    kotlin("multiplatform") version "2.3.21" apply false
}
