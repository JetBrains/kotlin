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

        val otherDist by configurations.creating {
            isCanBeConsumed = true
            isCanBeResolved = false
        }
        dependencies {
            runtimeOnly(project(mapOf("path" to path, "configuration" to otherDist.name)))
        }
        artifacts {
            add(otherDist.name, tasks.named("otherKlib").map { it.outputs.files.files.first() })
        }
        useCommonJs()
        browser {
        }
    }

    sourceSets {
        jsMain {
            kotlin.exclude("**/other/**")
        }
        val other by getting {
            kotlin.srcDirs("src/main/kotlin/other")
            dependencies {
                implementation(project(path = project.path))
            }
        }
    }
}