plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("test-inputs-check-v2")
}

publish()
sourcesJar()
javadocJar()
configureKotlinCompileTasksGradleCompatibility()

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$coreDepsVersion")
    testImplementation(kotlin("stdlib", coreDepsVersion))
    testImplementation(kotlin("test-junit5", coreDepsVersion))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks {
    apiBuild {
        inputJar.value(jar.flatMap { it.archiveFile })
    }
    test {
        useJUnitPlatform()
    }
}
