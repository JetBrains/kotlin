plugins {
    id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")
}

repositories {
    mavenLocal()
    jcenter()
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
            binaries {
                sharedLib {
                    export(project(":exported"))
                }
                staticLib {
                    export(project(":exported"))
                }
            }
        }
    }
}
