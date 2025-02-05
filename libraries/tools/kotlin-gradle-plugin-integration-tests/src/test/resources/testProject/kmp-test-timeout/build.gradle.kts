import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.time.Duration

plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js {
        nodejs {
            testTask {
                useMocha {
                    timeout = Duration.ofMinutes(5).toMillis().toString()
                }
            }
        }
    }
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        nodejs {
            testTask {
                useMocha {
                    timeout = Duration.ofMinutes(5).toMillis().toString()
                }
            }
        }
    }
    linuxX64()
    mingwX64()
    macosX64()
    macosArm64()
    linuxArm64()

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
