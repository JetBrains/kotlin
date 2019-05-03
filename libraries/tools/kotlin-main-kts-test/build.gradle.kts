
description = "Kotlin \"main\" script definition tests"

plugins {
    kotlin("jvm")
}

dependencies {
    if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
        testCompile(projectRuntimeJar(":kotlin-main-kts")) // runtimeJar needed to for proper dependency on the jar with relocations
    }

    testCompile(project(":kotlin-scripting-jvm-host-embeddable"))
    testCompile(commonDep("junit"))
    compileOnly("org.apache.ivy:ivy:2.4.0") // for jps/pill

    if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
        testCompile(project(":kotlin-scripting-jvm-host"))
        testCompile(project(":kotlin-main-kts"))
    }
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

projectTest(parallel = true)
