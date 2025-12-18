plugins {
    kotlin("multiplatform")
}

kotlin {
    js {
        val otherCompilation = compilations.create("other")
        tasks.register<Zip>("otherKlib") {
            from(otherCompilation.output.allOutputs)
            archiveExtension.set("klib")
        }

        useCommonJs()
        browser {
        }
    }

    sourceSets {
        jsMain {
            kotlin.exclude("**/other/**")
            dependencies {
                runtimeOnly(files(tasks.named("otherKlib")))
            }
        }
        val jsOther by getting {
            kotlin.srcDirs("src/other/kotlin/other")
            dependencies {
                implementation(project(path = project.path))
            }
        }
    }
}