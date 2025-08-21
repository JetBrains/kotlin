abstract class GenerateSourcesTask : org.gradle.api.DefaultTask() {
    @get:org.gradle.api.tasks.OutputDirectory
    abstract val outputDirectory: org.gradle.api.file.DirectoryProperty

    @org.gradle.api.tasks.TaskAction
    fun generate() {
        outputDirectory.asFile.get().mkdirs()
        outputDirectory.file("Generated.kt").get().asFile.writeText("""
                        public class Generated { public fun helloCreator(): Int = 42 }
                    """.trimIndent())
    }
}

val srcgen = project.tasks.register("generateSources", GenerateSourcesTask::class.java)
srcgen.configure {
    outputDirectory.set(project.layout.buildDirectory.get().dir("generated").dir("kotlin"))
}

val kotlin = project.extensions.getByType(org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension::class.java)
kotlin.sourceSets.getByName("commonMain") {
    kotlin.srcDir(srcgen)
}
