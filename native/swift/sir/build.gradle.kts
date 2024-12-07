plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("generated-sources")
}

description = "Swift Intermediate Representation"

dependencies {
    compileOnly(kotlinStdlib())

    if (kotlinBuildProperties.isInIdeaSync) {
        compileOnly(project("tree-generator")) // Provided, so that IDEA can recognize references to this module in KDoc.
    }
}

sourceSets {
    "main" { projectDefault() }
}

generatedSourcesTask(
    taskName = "generateTree",
    generatorProject = ":native:swift:sir:tree-generator",
    generatorRoot = "native/swift/sir/tree-generator/src",
    generatorMainClass = "org.jetbrains.kotlin.sir.tree.generator.MainKt",
)

publish()

runtimeJar()
sourcesJar()
javadocJar()
