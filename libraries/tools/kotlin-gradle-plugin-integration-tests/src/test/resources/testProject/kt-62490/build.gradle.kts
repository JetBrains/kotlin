plugins {
    `java`
    id("org.jetbrains.kotlin.jvm") apply false
}

abstract class CustomResGenerator : DefaultTask() {
    @get:OutputDirectory
    abstract val outputFile: DirectoryProperty

    @TaskAction
    fun doGenerate() {
        val dir = outputFile.get().asFile
        dir.mkdirs()
        dir.resolve("myres.txt").writeText("Banana!")
    }
}

val customResGenerator = tasks.register<CustomResGenerator>("customResGenerator") {
    outputFile.value(project.layout.buildDirectory.dir("custom-res"))
}

sourceSets.getByName("main").resources.srcDir(customResGenerator)

plugins.apply("org.jetbrains.kotlin.jvm")
