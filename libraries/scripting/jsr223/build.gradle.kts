import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
}

publish()

dependencies {
    api(project(":kotlin-script-runtime"))
    api(kotlinStdlib())
    api(project(":kotlin-scripting-common"))
    api(project(":kotlin-scripting-jvm"))
    api(project(":kotlin-scripting-jvm-host-unshaded"))
    api(project(":kotlin-scripting-compiler"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    compileOnly(intellijCore())
    publishedRuntime(project(":kotlin-compiler"))
    publishedRuntime(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xallow-kotlin-package")
}

standardPublicJars()

projectTest(parallel = true)

