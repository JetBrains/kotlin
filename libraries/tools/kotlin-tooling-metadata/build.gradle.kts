import gradle.enableKotlinSerializationPlugin

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    java
    kotlin("jvm")
    id("test-inputs-check-v2")
}

publish()
sourcesJar()
javadocJar()
configureKotlinCompileTasksGradleCompatibility()
enableKotlinSerializationPlugin()

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    implementation(kotlin("stdlib", coreDepsVersion))
    // We have to use earlier serialization version to prevent runtime conflicts
    // as per https://github.com/Kotlin/kotlinx.serialization/issues/2968
    // We still have this early version in runtime because of Kotlin 2.4 dependencies
    // See also KT-87453
    compileOnly(libs.kotlinx.serialization.core.jvm) {
        version {
            strictly(libs.versions.kotlinx.serialization.min.get())
        }
    }
    implementation(libs.kotlinx.serialization.json)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
