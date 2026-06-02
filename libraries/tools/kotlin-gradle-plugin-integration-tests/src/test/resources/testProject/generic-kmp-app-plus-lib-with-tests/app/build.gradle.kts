plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    js() {
        nodejs()
    }
    <SingleNativeTarget>("native")

    sourceSets {
        val commonMain = getByName("commonMain")
        commonMain.dependencies {
            implementation(project(":lib"))
        }

        val commonTest = getByName("commonTest")
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
