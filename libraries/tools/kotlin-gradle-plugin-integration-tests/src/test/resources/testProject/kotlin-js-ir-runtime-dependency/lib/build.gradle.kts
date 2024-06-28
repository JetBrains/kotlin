plugins {
    kotlin("js")
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
        val main by getting {
            kotlin.exclude("**/other/**")
            dependencies {
                runtimeOnly(files(tasks.named("otherKlib")))
            }
        }
        val other by getting {
            kotlin.srcDirs("src/main/kotlin/other")
            dependencies {
                implementation(project(path = project.path))
            }
        }
    }
}