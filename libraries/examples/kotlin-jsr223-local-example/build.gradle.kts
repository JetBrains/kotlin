
description = "Sample Kotlin JSR 223 scripting jar with local (in-process) compilation and evaluation"

plugins {
    kotlin("jvm")
}

dependencies {
    compile(project(":kotlin-stdlib"))
    compile(project(":kotlin-script-runtime"))
    compile(projectRuntimeJar(":kotlin-compiler-embeddable"))
    compile(project(":kotlin-script-util"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))
    testRuntime(project(":kotlin-reflect"))
}

projectTest()
