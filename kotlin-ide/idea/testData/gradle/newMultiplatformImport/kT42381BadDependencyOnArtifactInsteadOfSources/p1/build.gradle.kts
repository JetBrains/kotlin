plugins {
    kotlin("multiplatform")
}
kotlin {
    jvm()
    js() // arbitrary secondary target
    sourceSets {
        named("commonMain") {
            dependencies {
                implementation(project(":p2"))
            }
        }
    }
}
