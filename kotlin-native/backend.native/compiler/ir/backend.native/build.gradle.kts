import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
}

dependencies {
    api(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil")) { isTransitive = false }
    api(commonDependency("org.jetbrains.intellij.deps:log4j")) { isTransitive = false }
    api(commonDependency("org.jetbrains.intellij.deps:jdom"))
    api(commonDependency("com.fasterxml:aalto-xml")) { isTransitive = false }
    api(commonDependency("org.codehaus.woodstox:stax2-api")) { isTransitive = false }

    api(project(":native:objcexport-header-generator"))
    api(project(":native:objcexport-header-generator-k1"))
    api(project(":native:base"))

    api(project(":kotlin-native:llvmInterop", "llvmInteropStubs"))
    api(project(":native:kotlin-native-utils"))
    api(project(":core:descriptors"))
    api(project(":core:compiler.common.native"))
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.backend.common"))
    api(project(":compiler:ir.inline"))
    api(project(":compiler:ir.objcinterop"))
    api(project(":compiler:util"))
    api(project(":native:frontend.native"))
    api(project(":compiler:cli-common"))
    api(project(":compiler:cli-base"))
    api(project(":compiler:cli"))
    api(project(":kotlin-util-klib"))
    api(project(":kotlin-util-klib-metadata"))
    api(project(":compiler:ir.serialization.common"))
    api(project(":compiler:ir.serialization.native"))
    api(project(":compiler:fir:fir-serialization"))
    api(project(":compiler:fir:native"))
    api(project(":compiler:ir.psi2ir"))

    api(intellijCore())
    compileOnly(jpsModel())
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.optIn.addAll(
            listOf(
                    "kotlin.RequiresOptIn",
                    "kotlinx.cinterop.BetaInteropApi",
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