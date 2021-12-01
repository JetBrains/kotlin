import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(project(":plugins:fir-plugin-prototype"))
    testApi(project(":compiler:incremental-compilation-impl"))
    testApi(projectTests(":compiler:incremental-compilation-impl"))

    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testCompileOnly(project(":kotlin-reflect-api"))

    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(project(":compiler:fir:fir-serialization"))

    testRuntimeOnly(intellijDep()) {
        includeJars(
            "lz4-java",
            "jna",
            "jdom",
            "trove4j",
            "intellij-deps-fastutil-8.4.1-4",
            rootProject = rootProject)
    }

    testRuntimeOnly(toolsJar())
}

val generationRoot = projectDir.resolve("tests-gen")

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {
        projectDefault()
        this.java.srcDir(generationRoot.name)
    }
}

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    apply(plugin = "idea")
    idea {
        this.module.generatedSourceDirs.add(generationRoot)
    }
}

projectTest(parallel = true, jUnitMode = JUnitMode.JUnit4) {
    workingDir = rootDir
    useJUnitPlatform()
    jvmArgs!!.removeIf { it.contains("-Xmx") }
    maxHeapSize = "3g"
    dependsOn(":plugins:fir-plugin-prototype:jar")
    dependsOn(":plugins:fir-plugin-prototype:plugin-annotations:jar")
}

testsJar()
