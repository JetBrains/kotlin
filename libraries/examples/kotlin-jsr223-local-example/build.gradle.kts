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
    api(kotlinStdlib())
    api(project(":kotlin-script-runtime"))
    api(project(":kotlin-compiler-embeddable"))
    api(project(":kotlin-script-util"))
    runtimeOnly(project(":kotlin-scripting-compiler-embeddable"))
    testApi(project(":kotlin-test:kotlin-test-junit"))
    testApi(commonDependency("junit:junit"))
    compileOnly(project(":compiler:cli-common")) // TODO: fix import (workaround for jps build)
    testCompileOnly(project(":core:util.runtime")) // TODO: fix import (workaround for jps build)
    testCompileOnly(project(":daemon-common")) // TODO: fix import (workaround for jps build)
    testRuntimeOnly(project(":kotlin-scripting-compiler-embeddable"))
}

projectTest()
