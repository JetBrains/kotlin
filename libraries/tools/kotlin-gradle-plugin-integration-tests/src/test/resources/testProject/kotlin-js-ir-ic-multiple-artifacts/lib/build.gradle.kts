plugins {
    kotlin("js")
}

var conf: Configuration? = null

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
        conf = otherDist
        artifacts {
            add(otherDist.name, tasks.named("otherKlib").map { it.outputs.files.files.first() })
        }
        useCommonJs()
        browser {
        }
    }

    sourceSets {
        val main by getting {
            kotlin.exclude("**/other/**")
            dependencies {
                runtimeOnly(conf!!)
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