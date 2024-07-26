import org.jetbrains.kotlin.ideaExt.idea

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

repositories {
    maven {
        url = uri("https://packages.jetbrains.team/maven/p/kotlin-formver/maven")
    }
}

dependencies {
    embedded(project(":kotlin-formver-compiler-plugin.cli")) { isTransitive = false }
    embedded(project(":kotlin-formver-compiler-plugin.common")) { isTransitive = false }
    embedded(project(":kotlin-formver-compiler-plugin.core")) { isTransitive = false }
    embedded(project(":kotlin-formver-compiler-plugin.plugin")) { isTransitive = false }
    embedded(project(":kotlin-formver-compiler-plugin.viper")) { isTransitive = true }

    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(projectTests(":compiler:test-infrastructure"))

    testImplementation(project(":kotlin-formver-compiler-plugin.plugin"))
    testImplementation(project(":kotlin-formver-compiler-plugin.common"))

    testRuntimeOnly(project(":kotlin-formver-compiler-plugin.core"))
    testRuntimeOnly(project(":kotlin-formver-compiler-plugin.viper"))

    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))
}

optInToExperimentalCompilerApi()

val generationRoot = projectDir.resolve("tests-gen")

sourceSets {
    "main" {
        none()
    }
    "test" {
        projectDefault()
        this.java.srcDir(generationRoot.name)
    }
}

if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
    apply(plugin = "idea")
    idea {
        this.module.generatedSourceDirs.add(generationRoot)
    }
}

projectTest(parallel = true, jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(11))
        jvmArgs = listOf("-Xss30M")
    })
}.also { configureFormVerPluginAnnotationsDependency(it) }

runtimeJar()
sourcesJar()
javadocJar()
testsJar()