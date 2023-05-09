plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    <SingleNativeTarget>("native") {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
        compilations.configureEach {
            compilerOptions.configure {
                freeCompilerArgs.add("-Xpartial-linkage=disable")
            }
        }
        sourceSets["commonMain"].dependencies {
            implementation("io.ktor:ktor-client-core:1.5.4")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0-RC-native-mt")
        }
    }
}

val konanHome: String? by ext.properties
val kotlinNativeCompilerVersion = konanHome?.let { org.jetbrains.kotlin.konan.target.Distribution(it).compilerVersion }
    ?: "<pluginMarkerVersion>"

println("for_test_kotlin_native_compiler_version=$kotlinNativeCompilerVersion")
