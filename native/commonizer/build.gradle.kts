plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Kotlin KLIB Library Commonizer"

publish()

configurations {
    testRuntimeOnly {
        extendsFrom(compileOnly.get())
    }
}

dependencies {
    embedded(project(":kotlinx-metadata-klib")) { isTransitive = false }
    embedded(project(":kotlinx-metadata")) { isTransitive = false }

    // N.B. The order of "kotlinx-metadata*" dependencies makes sense for runtime classpath
    // of the "runCommonizer" task. Please, don't mix them up.
    compileOnly(project(":kotlinx-metadata-klib")) { isTransitive = false }
    compileOnly(project(":kotlinx-metadata")) { isTransitive = false }
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:ir.serialization.common"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":native:frontend.native"))
    compileOnly(project(":kotlin-util-klib-metadata"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("trove4j") }

    // This dependency is necessary to keep the right dependency record inside of POM file:
    publishedCompile(project(":kotlin-compiler"))

    api(kotlinStdlib())

    testImplementation(commonDep("junit:junit"))
    testImplementation(projectTests(":compiler:tests-common"))
}

val runCommonizer by tasks.registering(JavaExec::class) {
    classpath(configurations.compileOnly, sourceSets.main.get().runtimeClasspath)
    main = "org.jetbrains.kotlin.descriptors.commonizer.cli.CommonizerCLI"
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
}

runtimeJar()
sourcesJar { includeEmptyDirs = false; eachFile { exclude() } } // empty Jar, no public sources
javadocJar { includeEmptyDirs = false; eachFile { exclude() } } // empty Jar, no public javadocs
