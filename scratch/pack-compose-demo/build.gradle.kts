import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    kotlin("jvm") version "2.4.255-SNAPSHOT"
    kotlin("plugin.compose") version "2.4.255-SNAPSHOT"
    id("org.jetbrains.compose") version "1.7.3"
}

kotlin {
    jvmToolchain(21)
}

val jdk21Launcher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(21))
}

val jdk21Home = jdk21Launcher.get().metadata.installationPath.asFile.absolutePath
val visualVerificationImage = layout.buildDirectory.file("reports/visual-verification/pack-compose-demo.png")

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
}

compose.desktop {
    application {
        mainClass = "demo.MainKt"
        javaHome = jdk21Home
    }
}

tasks.register<JavaExec>("verifyVisualDemo") {
    group = "verification"
    description = "Launches the Compose spread-pack demo, captures a screenshot, and exits."

    dependsOn(tasks.named("classes"))
    javaLauncher.set(jdk21Launcher)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("demo.MainKt")
    systemProperty("compose.application.configure.swing.globals", "true")
    systemProperty("spread.pack.demo.screenshot", visualVerificationImage.get().asFile.absolutePath)
    outputs.file(visualVerificationImage)
}
