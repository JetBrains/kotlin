plugins {
    id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")
}

repositories {
    mavenLocal()
    jcenter()
    maven { setUrl(rootProject.projectDir.resolve("repo")) }
}

kotlin {
    ios()

    // Check that we can reenter the configuration method.
    ios {
        binaries.framework(listOf(DEBUG))
    }

    sourceSets["iosMain"].dependencies {
        implementation("common.ios:lib:1.0")
    }
}
