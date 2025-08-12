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

    maxHeapSize = "64m"
    arguments = arguments + listOf(
        "-visitor",
        "-long-messages",
        "-package", outputPackage
    )

    outputDirectory = layout.projectDirectory.dir("src/${outputPackage.replace('.', '/')}").asFile
}

tasks.compileKotlin {
    dependsOn(tasks.generateGrammarSource)
}
