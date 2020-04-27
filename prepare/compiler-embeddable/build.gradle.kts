
description = "Kotlin Compiler (embeddable)"

plugins {
    kotlin("jvm")
}

val testCompilationClasspath by configurations.creating

dependencies {
    runtimeOnly(kotlinStdlib())
    runtimeOnly(project(":kotlin-script-runtime"))
    runtimeOnly(project(":kotlin-reflect"))
    runtime(project(":kotlin-daemon-embeddable"))
    runtimeOnly(commonDep("org.jetbrains.intellij.deps", "trove4j"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompilationClasspath(kotlinStdlib())
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

publish()

noDefaultJar()

// dummy is used for rewriting dependencies to the shaded packages in the embeddable compiler
compilerDummyJar(compilerDummyForDependenciesRewriting("compilerDummy") {
    classifier = "dummy"
})

val runtimeJar = runtimeJar(embeddableCompiler()) {
    exclude("com/sun/jna/**")
    exclude("org/jetbrains/annotations/**")
    mergeServiceFiles()
}

sourcesJar()
javadocJar()

projectTest {
    dependsOn(runtimeJar)
    doFirst {
        val runtimeJarConfig = configurations["runtimeJar"]
        val runtimeConfig = configurations["runtime"]
        systemProperty("compilerClasspath", "${runtimeJarConfig.allArtifacts.files.files.first().path}${File.pathSeparator}${runtimeConfig.asPath}")
        systemProperty("compilationClasspath", testCompilationClasspath.asPath)
    }
}


