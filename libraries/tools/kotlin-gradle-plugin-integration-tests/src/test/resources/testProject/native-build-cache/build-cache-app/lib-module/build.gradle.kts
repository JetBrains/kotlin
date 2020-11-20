plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    mavenLocal()
    jcenter()
}

kotlin {
    linuxX64("host")
}
