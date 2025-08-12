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
    val outputPackage = "org.jetbrains.kotlin.js.parser.antlr"

    maxHeapSize = "64m"
    arguments = arguments + listOf(
        "-visitor",
        "-long-messages",
        "-package", outputPackage
    )

    outputDirectory = file("${layout.buildDirectory.get().asFile}/generated-src/antlr/main/${outputPackage.replace('.', '/')}")
}

tasks.compileKotlin {
    dependsOn(tasks.generateGrammarSource)
}
