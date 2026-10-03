import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

jvmToolchains {
    targetBytecodeVersion = JdkMajorVersion.JDK_1_8
}

dependencies {
    compileOnly(project(":kotlin-script-runtime")) // only for the deprecated jvm/compat/diagnosticsUtil.kt
    runtimeOnly(project(":kotlin-script-runtime")) // legacy templates support, to be dropped with the artifact deprecation
    api(kotlinStdlib())
    api(project(":kotlin-scripting-common"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xallow-kotlin-package")
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
