plugins {
    kotlin("multiplatform")
}

group = "org.sample.root"

kotlin {
    linuxX64("linux")
    js {
        nodejs()
    }

    sourceSets["commonMain"].dependencies {
        implementation(project(":foo"))
    }
}
