plugins {
    id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("<LocalRepo>")
}

kotlin {
    <SingleNativeTarget>("native") {
        binaries {
            executable {
                entryPoint = "sample.app.main"
            }
        }
        sourceSets["commonMain"].dependencies {
            implementation("org.sample:libb:1.0") // libb:1.0 is compatible with liba:1.0 only!
            implementation("org.sample:liba:2.0") // liba:1.0 -> liba:2.0
        }
    }
}

val konanHome: String? by ext.properties
val kotlinNativeCompilerVersion = konanHome?.let { org.jetbrains.kotlin.konan.target.Distribution(it).compilerVersion }
    ?: "<pluginMarkerVersion>"

println("for_test_kotlin_native_target=<SingleNativeTarget>")
println("for_test_kotlin_native_compiler_version=$kotlinNativeCompilerVersion")
