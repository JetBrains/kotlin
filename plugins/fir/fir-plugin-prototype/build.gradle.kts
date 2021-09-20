import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:fir:cones"))
    api(project(":compiler:fir:tree"))
    api(project(":compiler:fir:resolve"))
    api(project(":compiler:fir:checkers"))
    api(intellijCoreDep()) { includeJars("intellij-core") }

    compileOnly(project(":kotlin-reflect-api"))

    testApiJUnit5()
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(project(":compiler:fir:checkers"))
    testApi(project(":compiler:fir:checkers:checkers.jvm"))
    testApi(project(":compiler:frontend"))

    testCompileOnly(project(":kotlin-reflect-api"))
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":core:descriptors.runtime"))

    testRuntimeOnly(intellijDep()) {
        includeJars("jna", rootProject = rootProject)
    }

    testRuntimeOnly(intellijDep()) { includeJars(
        "intellij-deps-fastutil-8.4.1-4",
        "trove4j",
        "jdom"
    ) }
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

projectTest(parallel = true, jUnit5Enabled = true) {
    workingDir = rootDir
    jvmArgs!!.removeIf { it.contains("-Xmx") }
    maxHeapSize = "3g"
    dependsOn(":plugins:fir:fir-plugin-prototype:plugin-annotations:jar")
}

testsJar()
