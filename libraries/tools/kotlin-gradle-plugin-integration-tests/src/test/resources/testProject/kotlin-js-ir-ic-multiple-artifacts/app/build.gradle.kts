import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary

plugins {
    kotlin("js")
}

dependencies {
    implementation(project(":lib"))
    testImplementation(kotlin("test-js"))
}

kotlin {
    js {
        browser {
        }
        binaries.executable()
        val main by compilations.getting
        main.binaries
            .matching { it.mode == KotlinJsBinaryMode.DEVELOPMENT }
            .matching { it is JsIrBinary }
            .all  {
                this as JsIrBinary
                linkTask.configure {
                    val rootCacheDir = rootCacheDirectory.get()
                    rootCacheDirectory.set(rootCacheDir)
                }
            }
    }
}
tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile> {
    kotlinOptions.freeCompilerArgs += "-Xforce-deprecated-legacy-compiler-usage"
}
