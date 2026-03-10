import org.gradle.api.tasks.SourceSet
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

description = "Kotlin Standard Library JDK 7 extension"

plugins {
    kotlin("jvm")
}

configureJvmToolchain(JdkMajorVersion.JDK_1_8)

publish()
sourcesJar()
javadocJar()

val java9: SourceSet = sourceSets.create("java9") {
    java.srcDir("java9")
}

dependencies {
    api(project(":kotlin-stdlib"))
}

tasks.named<Jar>("jar") {
    project.manifestAttributes(manifest, "Main", true)
    from(java9.output)
}

tasks.named<Jar>("sourcesJar") {
    from(java9.allSource)
}

tasks.named<KotlinJvmCompile>("compileKotlin") {
    kotlinJavaToolchain.toolchain.use(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(JdkMajorVersion.JDK_1_8.targetName))
        moduleName.set(project.name)
        freeCompilerArgs.set(listOf("-Xjdk-release=7"))
    }
}

configureJava9Compilation("kotlin.stdlib.jdk7")
