plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    ios()
    watchos()
    tvos()
    macos {
        binaries.executable {
            entryPoint = "main"
        }
    }

    val commonTest by sourceSets.getting
    val jvmTest by sourceSets.getting

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

