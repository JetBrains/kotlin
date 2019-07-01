
description = "Kotlin Compiler (embeddable)"

plugins {
    `java`
}

dependencies {
    runtime(kotlinStdlib())
    runtime(project(":kotlin-script-runtime"))
    runtime(project(":kotlin-reflect"))
    runtime(project(":kotlin-daemon-embeddable"))
    runtime(commonDep("org.jetbrains.intellij.deps", "trove4j"))
}

publish()

noDefaultJar()

// dummy is used for rewriting dependencies to the shaded packages in the embeddable compiler
compilerDummyJar(compilerDummyForDependenciesRewriting("compilerDummy") {
    classifier = "dummy"
})

runtimeJar(embeddableCompiler()) {
    exclude("com/sun/jna/**")
}

sourcesJar()
javadocJar()

