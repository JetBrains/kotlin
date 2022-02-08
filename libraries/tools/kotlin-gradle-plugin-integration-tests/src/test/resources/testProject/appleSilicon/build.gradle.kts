plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm()
    ios()
    watchos()
    tvos()
    iosSimulatorArm64()
    tvosSimulatorArm64()
    watchosSimulatorArm64()
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

    val commonTest by sourceSets.getting
    val jvmTest by sourceSets.getting
    val macosMain by sourceSets.creating
    val iosMain by sourceSets.getting
    val tvosMain by sourceSets.getting
    val watchosMain by sourceSets.getting

    val macosX64Main by sourceSets.getting { dependsOn(macosMain) }
    val macosArm64Main by sourceSets.getting { dependsOn(macosMain) }
    val iosSimulatorArm64Main by sourceSets.getting { dependsOn(iosMain) }
    val tvosSimulatorArm64Main by sourceSets.getting { dependsOn(tvosMain) }
    val watchosSimulatorArm64Main by sourceSets.getting { dependsOn(watchosMain) }

    commonTest.dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
    }

    jvmTest.dependencies {
        implementation(kotlin("test-junit"))
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

