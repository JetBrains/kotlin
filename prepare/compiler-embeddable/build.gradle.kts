
description = "Kotlin Compiler (embeddable)"

plugins {
    kotlin("jvm")
}

val testCompilationClasspath by configurations.creating
val testCompilerClasspath by configurations.creating {
    isCanBeConsumed = false
    extendsFrom(configurations["runtimeElements"])
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    }
}

dependencies {
    runtimeOnly(kotlinStdlib())
    runtimeOnly(project(":kotlin-script-runtime"))
    runtimeOnly(project(":kotlin-reflect"))
    runtimeOnly(project(":kotlin-daemon-embeddable"))
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
        systemProperty("compilerClasspath", "${runtimeJar.get().outputs.files.asPath}${File.pathSeparator}${testCompilerClasspath.asPath}")
        systemProperty("compilationClasspath", testCompilationClasspath.asPath)
    }
}


