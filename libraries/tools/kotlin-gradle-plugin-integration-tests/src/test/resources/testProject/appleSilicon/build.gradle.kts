plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm()
    iosArm64()
    iosX64()
    iosSimulatorArm64()
    watchosArm32()
    watchosArm64()
    watchosX64()
    watchosSimulatorArm64()
    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()

    macosX64 {
        binaries.executable {
            entryPoint = "main"
        }
    }

    macosArm64 {
        binaries.executable {
            entryPoint = "main"
        }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test-common"))
            implementation(kotlin("test-annotations-common"))
        }

        jvmTest.dependencies {
            implementation(kotlin("test-junit"))
        }

    }

    tasks.withType<AbstractTestTask>().configureEach {
        testLogging {
            showStandardStreams = true
        }
    }
}


allprojects {
    repositories {
        mavenCentral()
        google()
        mavenLocal()
    }
}

