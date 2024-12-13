plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    linuxX64()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.foo)
                api(projects.bar) { }
            }
        }
    }
}
