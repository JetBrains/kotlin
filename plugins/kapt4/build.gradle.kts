import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Annotation Processor for Kotlin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:plugin-api"))
    implementation(project(":kotlin-annotation-processing-compiler"))
    compileOnly(project(":kotlin-annotation-processing-base"))
    compileOnly(project(":analysis:analysis-api-standalone"))
    compileOnly(toolsJarApi())

    embedded(project(":kotlin-annotation-processing-compiler")) { isTransitive = false }

    // The following list of dependencies to be embedded is: analysis-api-standalone plus all its transitive dependencies used from this
    // module, excluding the Kotlin compiler and standard libraries. We don't bundle compiler and libraries because they are published
    // separately, but we do bundle analysis API modules because they are not published.
    // This list is error-prone because if the analysis API module structure changes, the kotlin-annotation-processing artifact might have
    // references to non-existing symbols and it won't work. Integration tests are supposed to check that though.
    // TODO (KT-61030): simplify this somehow and ensure an integration test checks it.
    embedded(project(":analysis:analysis-api-standalone")) { isTransitive = false }
    embedded(project(":analysis:analysis-api")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-fir")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-impl-base")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-impl-barebone")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-standalone:analysis-api-standalone-base")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-standalone:analysis-api-fir-standalone-base")) { isTransitive = false }
    embedded(project(":analysis:analysis-internal-utils")) { isTransitive = false }
    embedded(project(":analysis:low-level-api-fir")) { isTransitive = false }
    embedded(project(":analysis:symbol-light-classes")) { isTransitive = false }

    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testApi(projectTests(":kotlin-annotation-processing-compiler"))
    testImplementation(project(":analysis:analysis-api-standalone"))
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


allprojects {
    tasks.withType(KotlinCompile::class).all {
        kotlinOptions {
            freeCompilerArgs += "-Xcontext-receivers"
        }
    }
}
