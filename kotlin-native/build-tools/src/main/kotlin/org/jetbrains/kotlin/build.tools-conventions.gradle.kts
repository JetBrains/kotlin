//plugins {
//    id("org.gradle.kotlin.kotlin-dsl")
//}
//
//gradlePlugin {
//    plugins {
//        create("compileToBitcode") {
//            id = "compile-to-bitcode"
//            implementationClass = "CompileToBitcodePlugin"
//        }
//        create("runtimeTesting") {
//            id = "runtime-testing"
//            implementationClass = "RuntimeTestingPlugin"
//        }
//        create("compilationDatabase") {
//            id = "compilation-database"
//            implementationClass = "CompilationDatabasePlugin"
//        }
//        create("konan") {
//            id = "konan"
//            implementationClass = "org.jetbrains.kotlin.gradle.plugin.konan.KonanPlugin"
//        }
//        // We bundle a shaded version of kotlinx-serialization plugin
//        create("kotlinx-serialization-native") {
//            id = "kotlinx-serialization-native"
//            implementationClass = "shadow.org.jetbrains.kotlinx.serialization.gradle.SerializationGradleSubplugin"
//        }
//
//        create("native") {
//            id = "native"
//            implementationClass = "org.jetbrains.gradle.plugins.tools.NativePlugin"
//        }
//
//        create("native-interop-plugin") {
//            id = "native-interop-plugin"
//            implementationClass = "org.jetbrains.kotlin.NativeInteropPlugin"
//        }
//    }
//}
