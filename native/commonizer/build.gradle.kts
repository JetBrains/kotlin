import org.gradle.api.artifacts.maven.Conf2ScopeMappingContainer.COMPILE

plugins {
    maven
    kotlin("jvm")
    id("jps-compatible")
}

val mavenCompileScope by configurations.creating {
    the<MavenPluginConvention>()
        .conf2ScopeMappings
        .addMapping(0, this, COMPILE)
}

configurations {
    runtimeOnly.get().extendsFrom(compileOnly.get())
}

description = "Kotlin KLIB Library Commonizer"

dependencies {
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:ir.serialization.common"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":native:frontend.native"))
    compileOnly(project(":kotlin-util-klib-metadata"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    // This dependency is necessary to keep the right dependency record inside of POM file:
    mavenCompileScope(project(":kotlin-compiler"))

    api(kotlinStdlib())

    testImplementation(commonDep("junit:junit"))
    testImplementation(projectTests(":compiler:tests-common"))
}

val runCommonizer by tasks.registering(NoDebugJavaExec::class) {
    classpath(sourceSets.main.get().runtimeClasspath)
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

publish()

standardPublicJars()
