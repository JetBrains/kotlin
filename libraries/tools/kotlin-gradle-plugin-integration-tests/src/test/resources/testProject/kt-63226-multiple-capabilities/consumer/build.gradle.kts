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
        implementation("test:producer:1.0")
    }

    sourceSets.jvmTest.dependencies {
        implementation("test:producer:1.0") {
            capabilities {
                requireCapability("test:foo:1.0")
            }
        }
    }
}
