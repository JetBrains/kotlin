plugins {
    java
    kotlin("jvm")
    id("jps-compatible")
}

configurations {
    create("antlrTool")
}

dependencies {
    "antlrTool"("org.antlr:antlr4:${libs.versions.antlr.get()}")
    implementation(libs.antlr.runtime)

    api(kotlinStdlib())
    api(project(":js:js.ast"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

val generateJsParser by tasks.registering(JavaExec::class) {
    val outputPackage = "org.jetbrains.kotlin.js.parser.antlr.generated"
    val outputDir = layout.projectDirectory.dir("src/${outputPackage.replace('.', '/')}").asFile
    val grammarDir = layout.projectDirectory.dir("src/main/antlr").asFile

    description = "Generates Java sources from ANTLR grammars"
    group = "build"

    mainClass.set("org.antlr.v4.Tool")
    classpath = configurations["antlrTool"]

    args = listOf(
        "-visitor",
        "-long-messages",
        "-package", outputPackage,
        "-o", outputDir.absolutePath
    ) + grammarDir.listFiles { file -> file.extension == "g4" }!!.map { it.name }
    workingDir = grammarDir

    inputs.dir(grammarDir)
    outputs.dir(outputDir)

    // Disable caching since we're writing to the source directory for version control
    outputs.cacheIf { false }

    doLast {
        // Force LF line endings for generated files, since ANTLR doesn't have a way to force LF line endings before executing
        outputDir.walkTopDown()
            .filter { it.isFile && (it.extension == "java" || it.extension == "tokens" || it.extension == "interp") }
            .forEach { file ->
                val content = file.readText()
                val normalizedContent = content.replace("\r\n", "\n").replace("\r", "\n")
                file.writeText(normalizedContent)
            }
    }
}