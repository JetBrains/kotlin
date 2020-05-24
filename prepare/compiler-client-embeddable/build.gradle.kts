description = "Kotlin compiler client embeddable"

plugins {
    maven
    kotlin("jvm")
}

val testCompilerClasspath by configurations.creating
val testCompilationClasspath by configurations.creating

dependencies {
    embedded(project(":compiler:cli-common")) { isTransitive = false }
    embedded(project(":daemon-common")) { isTransitive = false }
    embedded(project(":daemon-common-new")) { isTransitive = false }
    embedded(projectRuntimeJar(":kotlin-daemon-client"))
    
    testCompile(project(":compiler:cli-common"))
    testCompile(project(":daemon-common"))
    testCompile(project(":daemon-common-new"))
    testCompile(projectRuntimeJar(":kotlin-daemon-client"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompilerClasspath(project(":kotlin-compiler"))
    testCompilerClasspath(project(":kotlin-scripting-compiler-unshaded"))
    testCompilerClasspath(project(":kotlin-daemon"))
    testCompilationClasspath(kotlinStdlib())
    testCompilationClasspath(project(":kotlin-script-runtime"))
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

projectTest {
    doFirst {
        systemProperty("kotlin.test.script.classpath", testSourceSet.output.classesDirs.joinToString(File.pathSeparator))
        systemProperty("compilerClasspath", testCompilerClasspath.asPath)
        systemProperty("compilationClasspath", testCompilationClasspath.asPath)
    }
}

publish()

runtimeJar()

sourcesJar()

javadocJar()
