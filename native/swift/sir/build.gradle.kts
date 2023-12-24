import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
}

description = "Swift Intermediate Representation"

dependencies {
    compileOnly(kotlinStdlib())

    if (kotlinBuildProperties.isInIdeaSync) {
        compileOnly(project("tree-generator")) // Provided, so that IDEA can recognize references to this module in KDoc.
    }

    testImplementation(kotlin("test-junit5"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

testsJar()

projectTest(jUnitMode = JUnitMode.JUnit5) {
    workingDir = rootDir
    useJUnitPlatform { }
}

val generatorClasspath by configurations.creating

dependencies {
    generatorClasspath(project("tree-generator"))
}

val generationRoot = projectDir.resolve("gen")

val generateTree by tasks.registering(NoDebugJavaExec::class) {

    val generatorRoot = "$projectDir/tree-generator/src/"

    val generatorConfigurationFiles = fileTree(generatorRoot) {
        include("**/*.kt")
    }

    inputs.files(generatorConfigurationFiles)
    outputs.dirs(generationRoot)

    args(generationRoot)
    workingDir = rootDir
    classpath = generatorClasspath
    mainClass.set("org.jetbrains.kotlin.sir.tree.generator.MainKt")
    systemProperties["line.separator"] = "\n"
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDir(generateTree)
    }
    "test" { projectDefault() }
}

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    apply(plugin = "idea")
    idea {
        this.module.generatedSourceDirs.add(generationRoot)
    }
}
