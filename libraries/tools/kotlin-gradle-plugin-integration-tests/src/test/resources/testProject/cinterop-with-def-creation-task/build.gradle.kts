plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
    mavenLocal()
}

val createDefFileTask = project.tasks.register("createDefFileTask", CreateDefFileTask::class) {
    defFile.set(project.layout.buildDirectory.file(project.file("def/cinterop.def").absolutePath))
}

kotlin {
    <SingleNativeTarget>("native") {
        binaries {
            executable()
        }
        compilations.getByName("main") {
            cinterops {
                val cinterop by creating {
                    definitionFile.set(createDefFileTask.flatMap { it.defFile })
                }
            }
        }
    }
}