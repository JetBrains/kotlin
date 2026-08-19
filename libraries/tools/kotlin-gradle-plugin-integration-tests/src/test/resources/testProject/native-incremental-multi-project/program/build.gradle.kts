plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

group = "MultiProject"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    <SingleNativeTarget>("host") {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }

    sourceSets {
        val commonMain = getByName("commonMain") {
            dependencies {
                implementation(project(":library"))
            }
        }
    }
}
