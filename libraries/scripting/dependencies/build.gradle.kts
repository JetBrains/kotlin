import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
}

jvmToolchains {
    targetBytecodeVersion = JdkMajorVersion.JDK_1_8
}

dependencies {
    api(kotlinStdlib())
    api(project(":kotlin-scripting-common"))

    testFixturesApi(project(":kotlin-scripting-common"))
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testFixturesImplementation(libs.kotlinx.coroutines.core)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation(libs.kotlinx.coroutines.core)
}

sourceSets {
    "main" { projectDefault() }
    "testFixtures" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xallow-kotlin-package")
}

tasks.test {
    useJUnitPlatform()
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
