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
        implementation(kotlin("stdlib-common"))
        implementation(project(":foo"))
    }

    sourceSets["jsMain"].dependencies {
        implementation(kotlin("stdlib-js"))
    }
}
