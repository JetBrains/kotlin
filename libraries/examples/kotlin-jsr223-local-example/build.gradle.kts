import org.jetbrains.kotlin.pill.PillExtension

description = "Sample Kotlin JSR 223 scripting jar with local (in-process) compilation and evaluation"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

pill {
    variant = PillExtension.Variant.FULL
}

dependencies {
    compile(kotlinStdlib())
    compile(project(":kotlin-script-runtime"))
    compile(project(":kotlin-compiler-embeddable"))
    compile(project(":kotlin-script-util"))
    runtime(project(":kotlin-scripting-compiler"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))
    testRuntime(project(":kotlin-reflect"))
    compileOnly(project(":compiler:cli-common")) // TODO: fix import (workaround for jps build)
    testCompileOnly(project(":core:util.runtime")) // TODO: fix import (workaround for jps build)
    testCompileOnly(project(":daemon-common")) // TODO: fix import (workaround for jps build)
    testRuntime(project(":kotlin-scripting-compiler"))
}

projectTest()
