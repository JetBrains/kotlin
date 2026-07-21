plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    if (System.getProperty("os.arch") == "aarch64") {
        macosArm64("native")
    } else {
        macosX64("native")
    }
}

dependencies {
    add(
        "kotlinNativeCompilerPluginClasspath",
        project(":compiler-plugin")
    )
}
