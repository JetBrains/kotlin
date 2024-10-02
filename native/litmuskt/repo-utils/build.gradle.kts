plugins {
    kotlin("multiplatform")
}

repositories {
    maven {
        url = uri("https://packages.jetbrains.team/maven/p/plan/litmuskt")
    }
}

kotlin {
    linuxX64()
    linuxArm64()
    macosX64()
    macosArm64()
    mingwX64()

    jvm {
        withJava()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.litmuskt:litmuskt-core:0.1")
            }
        }
    }
}
