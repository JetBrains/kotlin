import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("root-config")
    kotlin("jvm")
}

dependencies {
    implementation(kotlinStdlib("jdk8"))
    ProjectModuleLists.kotlinJpsPluginEmbeddedDependencies.forEach { implementation(project(it)) }
    ProjectModuleLists.kotlinJpsPluginMavenDependencies.forEach { implementation(project(it)) }
    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    compileOnly(intellijUtilRt())
    compileOnly(intellijPlatformUtil())
    compileOnly(jpsModel())
    compileOnly(jpsModelImpl())
    compileOnly(jpsModelSerialization())
    compileOnly(intellijJDom())
    testCompileOnly(intellijJDom())

    testImplementation(project(":compiler:cli-base"))
    testImplementation(jpsModelSerialization())
    testImplementation(libs.junit4)
    testImplementation(kotlin("test-junit"))
}

sourceSets {
    "main" {
        projectDefault()
        generatedDir()
    }
    "test" { projectDefault() }
}

runtimeJar()
