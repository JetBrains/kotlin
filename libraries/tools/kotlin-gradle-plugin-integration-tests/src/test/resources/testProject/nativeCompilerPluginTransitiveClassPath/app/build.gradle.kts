import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    when {
        HostManager.hostIsMac && System.getProperty("os.arch") == "aarch64" -> macosArm64("native")
        HostManager.hostIsMac -> macosX64("native")
        HostManager.hostIsLinux -> linuxX64("native")
        else -> error("Unexpected host: ${HostManager.host}")
    }
}

dependencies {
    add(
        "kotlinNativeCompilerPluginClasspath",
        project(":compiler-plugin")
    )
}
