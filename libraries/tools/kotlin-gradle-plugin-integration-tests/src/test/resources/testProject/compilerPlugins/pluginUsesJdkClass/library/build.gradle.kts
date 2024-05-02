plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64()
}

dependencies {
    add("kotlinNativeCompilerPluginClasspath", project(":plugin"))
}
