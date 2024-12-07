plugins {
    kotlin("multiplatform")
}

repositories {
    maven(rootDir.resolve("repo"))
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm()

    sourceSets.jvmMain.dependencies {
        implementation("org.jetbrains.sample:producer:1.0.0")
        implementation("org.jetbrains.sample:producer:1.0.0") {
            capabilities {
                requireCapability("org.jetbrains.sample:producer-foo")
            }
        }
    }

    sourceSets.jvmTest.dependencies {
        implementation(project.dependencies.testFixtures("org.jetbrains.sample:producer:1.0.0"))
    }
}
