import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Annotation Processor for Kotlin K2"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":compiler:cli"))
    api(project(":compiler:backend"))
    api(project(":compiler:frontend"))
    api(project(":compiler:frontend.java"))
    api(project(":compiler:plugin-api"))

    embedded(project(":analysis:analysis-api-standalone")) {
        exclude("org.jetbrains.kotlin", "kotlin-stdlib")
        exclude("org.jetbrains.kotlin", "kotlin-stdlib-common")
    }

    compileOnly(project(":kotlin-annotation-processing"))
    embedded(project(":kotlin-annotation-processing")) { isTransitive = false }

    embedded(project(":kotlin-annotation-processing-base")) { isTransitive = false }
    testImplementation(project(":kotlin-annotation-processing-cli"))
    embedded(project(":kotlin-annotation-processing-runtime")){ isTransitive = false }
    implementation(project(":compiler:backend.jvm.entrypoint"))

    compileOnly(toolsJarApi())
    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))

    testImplementation(intellijCore())
    testRuntimeOnly(intellijResources()) { isTransitive = false }

    testApiJUnit5()
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":kotlin-annotation-processing"))
    testApi(projectTests(":kotlin-annotation-processing-cli"))

    testApi(projectTests(":kotlin-annotation-processing-base"))

    testCompileOnly(toolsJarApi())
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
//kaptTestTask("testJdk11", JavaLanguageVersion.of(11))

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
