import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    jvm()
    js {
        nodejs()
    }
    macosArm64()

    sourceSets.commonTest.dependencies {
        implementation(kotlin("test"))
        api(project(":bar"))
    }
}
