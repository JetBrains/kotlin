plugins {
    id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { setUrl(rootProject.projectDir.resolve("repo")) }
}

kotlin {
    macos()

    // Check that we can reenter the configuration method.
    macos {
        binaries.framework(listOf(DEBUG))
    }

    sourceSets["macosMain"].dependencies {
        implementation("common.macos:lib:1.0")
    }
}
