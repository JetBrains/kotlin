import org.jetbrains.kotlin.kotlinNativeDist

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(intellijCore())
    implementation(project(":compiler:cli-base"))
    implementation(project(":compiler:cli-common"))
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:ir.objcinterop"))
    implementation(project(":compiler:ir.serialization.common"))
    implementation(project(":compiler:ir.serialization.native"))
    implementation(project(":compiler:ir.tree"))
    implementation(project(":core:compiler.common.native"))
    implementation(project(":core:descriptors"))
    implementation(project(":kotlin-native:model"))
    implementation(project(":native:kotlin-native-utils"))

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(project(":compiler:tests-common", "tests-jar"))
    testImplementation(project(":kotlin-native:backend.native"))

    testRuntimeOnly(libs.junit.jupiter.engine)
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.backend.konan.InternalKonanApi")
    }
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    useJUnitPlatform()
    dependsOn(":kotlin-native:dist")
    systemProperty("org.jetbrains.kotlin.native.home", kotlinNativeDist.canonicalPath)
    workingDir(rootProject.projectDir)
}
