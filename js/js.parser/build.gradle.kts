plugins {
    java
    kotlin("jvm")
    id("jps-compatible")
    antlr
}

dependencies {
    antlr("org.antlr:antlr4:4.13.2")
    implementation("org.antlr:antlr4-runtime:4.13.2")

    api(kotlinStdlib())
    api(project(":js:js.ast"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks.generateGrammarSource {
    val outputPackage = "org.jetbrains.kotlin.js.parser.antlr.generated"
    val outputDir = layout.projectDirectory.dir("src/${outputPackage.replace('.', '/')}").asFile

    maxHeapSize = "64m"
    arguments = arguments + listOf(
        "-visitor",
        "-long-messages",
        "-package", outputPackage
    )

    outputDirectory = outputDir

    // Force LF line endings for generated files, since ANTLR doesn't have a way to force LF line endings in advance
    doLast {
        outputDir.walkTopDown()
            .filter { it.isFile && (it.extension == "java" || it.extension == "tokens" || it.extension == "interp") }
            .forEach { file ->
                val content = file.readText()
                val normalizedContent = content.replace("\r\n", "\n").replace("\r", "\n")
                file.writeText(normalizedContent)
            }
    }
}

tasks.compileKotlin {
    dependsOn(tasks.generateGrammarSource)
}
