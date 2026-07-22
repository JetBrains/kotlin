plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
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

val generateJsParser by tasks.registering(JavaExec::class) {
    val outputPackage = "org.jetbrains.kotlin.js.parser.antlr.generated"
    val genSourceSet = layout.projectDirectory.dir("gen")
    val outputDir = genSourceSet.dir(outputPackage.replace('.', '/'))
    val grammarDir = layout.projectDirectory.dir("src/main/antlr").asFile

    description = "Generates Java sources from ANTLR grammars"
    group = "build"

    mainClass.set("org.antlr.v4.Tool")
    classpath = antlrTool

    // ANTLR 4.13 is compiled for Java 11, so the tool must run on a JDK 11+ launcher
    // regardless of the JVM Gradle itself is running on.
    javaLauncher.set(project.getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))

    val grammarFiles = grammarDir
        .walkTopDown()
        .filter { file -> file.extension == "g4" }
        .map { it.name }

    val outputArg = outputDir.asFile.relativeTo(grammarDir).path
    args = listOf(
        "-visitor",
        "-long-messages",
        "-package", outputPackage,
        "-o", outputArg
    ) + grammarFiles
    workingDir = grammarDir

    inputs.dir(grammarDir)
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("antlrGrammarDirectory")

    inputs.files(antlrTool)
        .withPropertyName("antlrToolClasspath")
        .withNormalizer(ClasspathNormalizer::class)

    outputs.dir(genSourceSet)
        .withPropertyName("generatedParserSources")

    doLast {
        // Force LF line endings for generated files on Windows, since ANTLR doesn't have a way to force LF line endings before executing
        outputDir.asFile.walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                val content = file.readText()
                val normalizedContent = content.replace("\r\n", "\n").replace("\r", "\n")
                file.writeText(normalizedContent)
            }
    }
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDirs(generateJsParser)
    }
    "test" {}
}

registerInAggregateGenerateSources("generateJsParser")
