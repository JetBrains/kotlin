import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension


fun Project.configureMpp() {
    val kotlinExtension = extensions.findByName("kotlin") as? KotlinMultiplatformExtension
    check(kotlinExtension != null) {
        "No multiplatform extension found"
    }
    kotlinExtension.apply {
        js(IR) {
            nodejs {
            }
        }
        @OptIn(ExperimentalWasmDsl::class)
        wasmJs {
            nodejs()
        }
    }

}