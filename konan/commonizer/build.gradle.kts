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

description = "Kotlin/Native library commonizer"

dependencies {
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:ir.serialization.common"))

    // This dependency is necessary to keep the right dependency record inside of POM file:
    mavenCompileScope(project(":kotlin-compiler"))

    compile(kotlinStdlib())

    compile(project(":kotlin-util-klib-metadata"))
    compile(project(":kotlin-native:kotlin-native-utils")) { isTransitive = false }
    compile(project(":kotlin-native:kotlin-native-library-reader")) { isTransitive = false }

    testCompile(commonDep("junit:junit"))
    testCompile(projectTests(":compiler:tests-common"))

    testCompile(intellijCoreDep()) { includeJars("intellij-core") }
    testCompile(intellijDep()) {
        includeJars(
            "openapi",
            "jps-model",
            "extensions",
            "util",
            "platform-api",
            "platform-impl",
            "idea",
            "idea_rt",
            "guava",
            "trove4j",
            "picocontainer",
            "asm-all",
            "log4j",
            "jdom",
            "streamex",
            "bootstrap",
            rootProject = rootProject
        )
        isTransitive = false
    }

    Platform[192].orHigher {
        testCompile(intellijDep()) { includeJars("platform-util-ui", "platform-concurrency", "platform-objectSerializer") }
    }
}

val runCommonizer by tasks.registering(NoDebugJavaExec::class) {
    classpath(sourceSets.main.get().runtimeClasspath)
    main = "org.jetbrains.kotlin.descriptors.commonizer.cli.NativeDistributionCommonizerKt"
}

sourceSets {
    "main" {
        projectDefault()
        runtimeClasspath += configurations.compileOnly
    }
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
}

publish()

standardPublicJars()
