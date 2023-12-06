buildscript {
    repositories {
        mavenCentral()

        val kotlinCompilerRepo: String? by rootProject
        kotlinCompilerRepo?.let { maven(it) }
    }

    val kotlin_version: String by rootProject
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
    }
}

allprojects {
    repositories {
        mavenCentral()

        val kotlinCompilerRepo: String? by rootProject
        kotlinCompilerRepo?.let { maven(it) }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile>().configureEach {
        compilerOptions.freeCompilerArgs.addAll(
                "-XXLanguage:+ImplicitSignedToUnsignedIntegerConversion",
                "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                "-opt-in=kotlin.experimental.ExperimentalNativeApi"
        )
    }
}

val hostOs = System.getProperty("os.name")
val isMacos = hostOs == "Mac OS X"
val isLinux = hostOs == "Linux"
val isWindows = hostOs.startsWith("Windows")

val localRepo = rootProject.file("build/.m2-local")

val clean by tasks.creating(Delete::class) {
    delete(localRepo)
}

val buildSamplesWithPlatformLibs by tasks.creating {
    dependsOn(":csvparser:assemble")
    if (!isWindows) {
        dependsOn(":curl:assemble")
    }
    dependsOn(":globalState:assemble")
    dependsOn(":workers:assemble")

    if (isMacos || isLinux) {
        dependsOn(":echoServer:assemble") //https://youtrack.jetbrains.com/issue/KT-63721/
        dependsOn(":nonBlockingEchoServer:assemble")
        dependsOn(":tensorflow:assemble")
    }

    if (isMacos) {
        dependsOn(":objc:assemble")
        dependsOn(":opengl:assemble")
        dependsOn(":uikit:assemble")
        dependsOn(":watchos:assemble")
    }

    if (isWindows) {
        dependsOn(":win32:assemble")
    }
}
