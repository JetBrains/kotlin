import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

description = "Annotation Processor for Kotlin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:cli"))
    compileOnly(intellijCore())
    compileOnly(toolsJarApi())
    implementation(project(":kotlin-annotation-processing-compiler"))
    compileOnly(project(":kotlin-annotation-processing-base"))

    embedded(project(":kotlin-annotation-processing-compiler")) { isTransitive = false }

    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testApi(projectTests(":kotlin-annotation-processing-compiler"))
    testRuntimeOnly(toolsJar())
    testRuntimeOnly(commonDependency("org.codehaus.woodstox:stax2-api"))
    testRuntimeOnly(commonDependency("com.fasterxml:aalto-xml"))
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

testsJar {}

kaptTestTask("test", JavaLanguageVersion.of(8))
kaptTestTask("testJdk11", JavaLanguageVersion.of(11))
kaptTestTask("testJdk17", JavaLanguageVersion.of(17))
kaptTestTask("testJdk21", JavaLanguageVersion.of(21))

fun Project.kaptTestTask(name: String, javaLanguageVersion: JavaLanguageVersion) {
    val service = extensions.getByType<JavaToolchainService>()

    projectTest(taskName = name, parallel = true) {
        useJUnitPlatform {
            excludeTags = setOf("IgnoreJDK11")
        }
        workingDir = rootDir
        dependsOn(":dist")
        javaLauncher.set(service.launcherFor { languageVersion.set(javaLanguageVersion) })
    }
}

publish()
runtimeJar()
sourcesJar()
javadocJar()

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xcontext-parameters")
}
