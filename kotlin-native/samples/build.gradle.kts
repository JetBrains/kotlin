buildscript {
    repositories {
        mavenCentral()
        maven("https://dl.bintray.com/kotlin/kotlin-dev")
        maven("https://dl.bintray.com/kotlin/kotlin-eap")

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
        maven("https://dl.bintray.com/kotlin/kotlin-dev")
        maven("https://dl.bintray.com/kotlin/kotlin-eap")

        val kotlinCompilerRepo: String? by rootProject
        kotlinCompilerRepo?.let { maven(it) }
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

val buildSh by tasks.creating(Exec::class) {
    errorOutput = System.out
    isIgnoreExitValue = true
    workingDir = projectDir
    enabled = !isWindows
    if (isLinux || isMacos) {
        commandLine = listOf(projectDir.resolve("build.sh").toString())
    }
}

val buildSamplesWithPlatformLibs by tasks.creating {
    dependsOn(":csvparser:assemble")
    dependsOn(":curl:assemble")
    dependsOn(":echoServer:assemble")
    dependsOn(":globalState:assemble")
    dependsOn(":html5Canvas:assemble")
    dependsOn(":workers:assemble")

    if (isMacos || isLinux) {
        dependsOn(":nonBlockingEchoServer:assemble")
        dependsOn(":tensorflow:assemble")
    }

    if (isMacos) {
        dependsOn(":objc:assemble")
        dependsOn(":opengl:assemble")
        dependsOn(":uikit:assemble")
        dependsOn(":coverage:assemble")
        dependsOn(":watchos:assemble")
    }

    if (isWindows) {
        dependsOn(":win32:assemble")
    }
}

val buildAllSamples by tasks.creating {
    subprojects.forEach {
        dependsOn("${it.path}:assemble")
    }
    finalizedBy(buildSh)
}
