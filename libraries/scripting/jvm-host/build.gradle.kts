
plugins {
    kotlin("jvm")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":kotlin-script-runtime"))
    compile(kotlinStdlib())
    compile(project(":kotlin-scripting-common"))
    compile(project(":kotlin-scripting-jvm"))
    compile(project(":kotlin-scripting-compiler"))
    compile(project(":kotlin-scripting-compiler-impl"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(intellijCoreDep())
    runtime(projectRuntimeJar(":kotlin-compiler"))
    runtime(project(":kotlin-reflect"))
    runtime(commonDep("org.jetbrains.intellij.deps", "trove4j"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit"))
    testCompile(project(":compiler:daemon-common")) // TODO: fix import (workaround for jps build)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

publish()

standardPublicJars()


projectTest(parallel = true) {
    workingDir = rootDir
}
