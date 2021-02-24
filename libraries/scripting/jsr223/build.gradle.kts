
plugins {
    kotlin("jvm")
}

publish()

dependencies {
    compile(project(":kotlin-script-runtime"))
    compile(kotlinStdlib())
    compile(project(":kotlin-scripting-common"))
    compile(project(":kotlin-scripting-jvm"))
    compile(project(":kotlin-scripting-jvm-host-unshaded"))
    compile(project(":kotlin-scripting-compiler"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(intellijCoreDep())
    publishedRuntime(project(":kotlin-compiler"))
    publishedRuntime(project(":kotlin-reflect"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions.freeCompilerArgs += "-Xallow-kotlin-package"
}

standardPublicJars()

projectTest(parallel = true)

