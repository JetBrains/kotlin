plugins {
    java
    kotlin("jvm")
}

val antlrTool by configurations.creating

dependencies {
    antlrTool(libs.antlr)
    api(libs.antlr.runtime)

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
    classpath = antlrTool

    val grammarFiles = grammarDir
        .walkTopDown()
        .filter { file -> file.extension == "g4" }
        .map { it.name }

    args = listOf(
        "-visitor",
        "-long-messages",
        "-package", outputPackage,
        "-o", outputDir.absolutePath
    ) + grammarFiles
    workingDir = grammarDir

    inputs.dir(grammarDir)
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("antlrGrammarDirectory")

    inputs.files(antlrTool)
        .withPropertyName("antlrToolClasspath")
        .withNormalizer(ClasspathNormalizer::class)

    outputs.dir(outputDir)
        .withPropertyName("generatedParserSources")

    outputs.cacheIf { true }

    doLast {
        // Force LF line endings for generated files on Windows, since ANTLR doesn't have a way to force LF line endings before executing
        outputDir.walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                val content = file.readText()
                val normalizedContent = content.replace("\r\n", "\n").replace("\r", "\n")
                file.writeText(normalizedContent)
            }
    }
}
