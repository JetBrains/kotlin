description = "Annotation Processor for Kotlin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

fun ModuleDependency.excludeGradleCommonDependencies() {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-script-runtime")
}

dependencies {
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:plugin-api"))
    implementation(project(":kotlin-annotation-processing-compiler"))
    compileOnly(project(":kotlin-annotation-processing-base"))
    compileOnly(project(":analysis:analysis-api-standalone"))

    embedded(project(":kotlin-annotation-processing-compiler")) { excludeGradleCommonDependencies() }

    // The following list of dependencies to be embedded is: analysis-api-standalone plus all its transitive dependencies used from this
    // module, excluding the Kotlin compiler and standard libraries. We don't bundle compiler and libraries because they are published
    // separately, but we do bundle analysis API modules because they are not published.
    // This list is error-prone because if the analysis API module structure changes, the kotlin-annotation-processing artifact might have
    // references to non-existing symbols and it won't work. Integration tests are supposed to check that though.
    // TODO (KT-61030): simplify this somehow and ensure an integration test checks it.
    embedded(project(":analysis:analysis-api-standalone")) { excludeGradleCommonDependencies() }
    embedded(project(":analysis:analysis-api")) { excludeGradleCommonDependencies() }
    embedded(project(":analysis:analysis-api-fir")) { excludeGradleCommonDependencies() }
    embedded(project(":analysis:analysis-api-impl-base")) { excludeGradleCommonDependencies() }
    embedded(project(":analysis:analysis-api-impl-barebone")) { excludeGradleCommonDependencies() }
    embedded(project(":analysis:analysis-api-standalone:analysis-api-standalone-base")) { excludeGradleCommonDependencies() }
    embedded(project(":analysis:analysis-api-standalone:analysis-api-fir-standalone-base")) { excludeGradleCommonDependencies() }
    embedded(project(":analysis:analysis-internal-utils")) { excludeGradleCommonDependencies() }
    embedded(project(":analysis:low-level-api-fir")) { excludeGradleCommonDependencies() }
    embedded(project(":analysis:symbol-light-classes")) { excludeGradleCommonDependencies() }
    embedded(intellijCore())

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

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xcontext-receivers"
}