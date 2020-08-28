description = "Kotlin compiler client embeddable"

plugins {
    kotlin("jvm")
}

val testCompilerClasspath by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    }
}

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
    testCompilerClasspath(commonDep("org.jetbrains.intellij.deps", "trove4j"))
    testCompilerClasspath(project(":kotlin-scripting-compiler"))
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
