plugins {
    id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")
}

repositories {
    mavenLocal()
    jcenter()
}

kotlin {
    <SingleNativeTarget>("host") {
        compilations.all {
            kotlinOptions.verbose = true
            enableEndorsedLibs = true
        }
    }
}
