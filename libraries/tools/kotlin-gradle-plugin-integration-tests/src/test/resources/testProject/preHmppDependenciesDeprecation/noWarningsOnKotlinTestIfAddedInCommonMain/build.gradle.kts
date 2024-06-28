plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("<localRepo>")
}

kotlin {
    jvm() {
    }
    linuxX64()

    sourceSets.getByName("commonMain").dependencies {
        implementation(kotlin("test"))
    }

    sourceSets.getByName("jvmMain").dependencies {
        implementation(kotlin("test-junit"))
    }
}
