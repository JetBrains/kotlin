plugins {
    kotlin("multiplatform")
}

abstract class GenerateSourceTask : DefaultTask() {
    @get:OutputDirectory
    val output: Provider<Directory> = project.layout.buildDirectory.dir("generatedSources")

    @get:Input
    abstract val version: Property<String>

    @TaskAction
    fun action() {
        output.get().file("source.kt").asFile.writeText(
            """
                class Version { fun value() = "${version.get()}" }
            """.trimIndent()
        )
    }
}

val generateSourceTask = tasks.register("generateSource", GenerateSourceTask::class) {
    version.set(providers.gradleProperty("generateTestValue"))
}

kotlin {
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "Kotlin"
            isStatic = true
        }
    }

    sourceSets.commonMain.configure {
        kotlin.srcDir(generateSourceTask)
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}