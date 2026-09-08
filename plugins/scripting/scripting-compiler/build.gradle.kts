import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

description = "Kotlin Scripting Compiler Plugin"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("test-inputs-check")
}

dependencies {
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:psi:psi-api"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:fir:entrypoint"))
    compileOnly(project(":compiler:fir:raw-fir:raw-fir.common"))
    compileOnly(project(":compiler:fir:tree"))
    compileOnly(project(":compiler:fir:providers"))
    compileOnly(project(":compiler:fir:fir2ir:jvm-backend"))
    compileOnly(project(":compiler:fir:plugin-utils"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:cli-jvm"))
    compileOnly(project(":core:descriptors.runtime"))
    compileOnly(project(":core:reflection.common.jvm"))
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(project(":compiler:backend.jvm.entrypoint"))
    compileOnly(project(":compiler:backend.common.jvm"))
    compileOnly(project(":compiler:serialization.common"))
    compileOnly(project(":compiler:serialization"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    api(project(":kotlin-scripting-common"))
    api(project(":kotlin-scripting-jvm"))
    api(project(":kotlin-scripting-compiler-impl"))
    api(kotlinStdlib())
    api(variantOf(libs.jline) { classifier("jdk8") })
    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)

    compileOnly(project(":core:descriptors"))
    compileOnly(project(":core:descriptors.jvm"))
    compileOnly(project(":core:deserialization"))
    compileOnly(project(":compiler:container"))
    compileOnly(project(":compiler:ir.psi2ir"))
    compileOnly(project(":compiler:resolution"))
    compileOnly(project(":kotlin-util-klib-metadata"))
    implementation(project(":kotlin-power-assert-compiler-plugin")) // TODO: KT-74787
    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
}

optInToExperimentalCompilerApi()
optInToK1Deprecation()
optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        progressiveMode.set(false)
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
