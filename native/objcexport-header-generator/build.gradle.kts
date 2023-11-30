plugins {
    kotlin("jvm")
}

publish {
    artifactId = "kotlin-objcexport-header-generator"
}

sourcesJar()
javadocJar()

dependencies {
    implementation(intellijCore())
    implementation(project(":compiler:cli-base"))
    implementation(project(":compiler:cli-common"))
    implementation(project(":compiler:ir.objcinterop"))
    implementation(project(":compiler:ir.serialization.native"))
    implementation(project(":core:compiler.common.native"))
    implementation(project(":core:descriptors"))
    implementation(project(":native:base"))
    implementation(project(":native:kotlin-native-utils"))

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(project(":compiler:tests-common", "tests-jar"))

    testRuntimeOnly(libs.junit.jupiter.engine)
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi")
    }
}

nativeTest("test", tag = null) {
    useJUnitPlatform()
    systemProperty("projectDir", projectDir.absolutePath)
    workingDir(rootProject.projectDir)
}
