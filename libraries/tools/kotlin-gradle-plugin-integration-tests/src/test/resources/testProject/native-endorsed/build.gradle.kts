plugins {
    id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")
}

repositories {
    mavenLocal()
    jcenter()
}

kotlin {
    val commonNative by sourceSets.creating {}

    <SingleNativeTarget>("host") {
        compilations["main"].defaultSourceSet {
            dependsOn(commonNative)
        }
        compilations.all {
            kotlinOptions.verbose = true
            enableEndorsedLibs = true
        }
        binaries {
            executable(listOf(DEBUG))
        }
    }
}
