import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    linuxX64()
    linuxArm64()

    targets.withType<KotlinNativeTarget>().configureEach {
        compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME) {
            cinterops {
                create("myinterop")
            }
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":lib"))
            }
        }
    }
}