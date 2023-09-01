plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    sourceSets["commonMain"].apply {
        dependencies {
            api("org.jetbrains.kotlin:kotlin-stdlib-common")
            api(project(":exported"))
        }
    }

    <SingleNativeTarget>("host") {
        binaries {
            sharedLib() {
                export(project(":exported"))
            }
            staticLib() {
                export(project(":exported"))
            }
        }
    }
}
