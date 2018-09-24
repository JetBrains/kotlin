import org.jetbrains.kotlin.gradle.dsl.Coroutines

plugins {
    kotlin("jvm")
}

dependencies {
    compile(project(":examples:scripting-jvm-maven-deps"))
    compile(project(":kotlin-scripting-jvm-host"))
    compile(project(":kotlin-stdlib"))
    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(project(":compiler:util"))
    runtime(project(":kotlin-reflect"))

    testRuntimeOnly(projectRuntimeJar(":kotlin-compiler"))
    testCompile(commonDep("junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
}
