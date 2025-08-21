import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:ir.tree"))

    compileOnly(jpsModel())
    compileOnly(project(":compiler:cli-common"))
    compileOnly(commonDependency("org.jetbrains.intellij.deps:log4j")) { isTransitive = false }

    implementation(commonDependency("com.fasterxml:aalto-xml")) { isTransitive = false }
    implementation(commonDependency("org.codehaus.woodstox:stax2-api")) { isTransitive = false }
    implementation(libs.intellij.fastutil) { isTransitive = false }
    implementation(intellijJDom())
    implementation(intellijCore())
    implementation(project(":compiler:cli"))
    implementation(project(":compiler:fir:fir-serialization"))
    implementation(project(":compiler:fir:fir-native"))
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:ir.backend.native"))
    implementation(project(":compiler:ir.inline"))
    implementation(project(":compiler:ir.objcinterop"))
    implementation(project(":compiler:ir.psi2ir"))
    implementation(project(":compiler:ir.serialization.common"))
    implementation(project(":compiler:ir.serialization.native"))
    implementation(project(":compiler:util"))
    implementation(project(":core:compiler.common.native"))
    implementation(project(":core:descriptors"))
    implementation(project(":kotlin-native:llvmInterop"))
    implementation(project(":kotlin-util-klib"))
    implementation(project(":kotlin-util-klib-metadata"))
    implementation(project(":native:base"))
    implementation(project(":native:frontend.native"))
    implementation(project(":native:kotlin-native-utils"))
    implementation(project(":native:objcexport-header-generator"))
    implementation(project(":native:objcexport-header-generator-k1"))
    implementation(project(":native:binary-options"))
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.optIn.addAll(
            listOf(
                    "kotlinx.cinterop.ExperimentalForeignApi",
                    "org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi",
                    "org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI"
            )
    )
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

sourcesJar()
javadocJar()
