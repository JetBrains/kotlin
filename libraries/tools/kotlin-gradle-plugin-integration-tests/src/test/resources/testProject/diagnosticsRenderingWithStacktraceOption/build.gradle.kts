plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm("customName")

    sourceSets {
        jvmMain { // should provoke PlatformSourceSetConventionUsedWithCustomTargetName

        }
    }
}
