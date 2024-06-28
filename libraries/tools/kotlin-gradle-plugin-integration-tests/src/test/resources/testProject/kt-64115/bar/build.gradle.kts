plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    jvm()
    js {
        nodejs()
    }
    macosArm64()

    sourceSets.commonMain.dependencies {
        api(project(":foo"))
    }
}
