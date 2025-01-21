import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Standalone Runner for Swift Export"

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    implementation(project(":native:swift:sir"))
    implementation(project(":native:swift:sir-compiler-bridge"))
    implementation(project(":native:swift:sir-light-classes"))
    implementation(project(":native:swift:sir-printer"))

    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-api-standalone"))

    implementation(project(":native:analysis-api-klib-reader"))

    testApi(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.api)

    testRuntimeOnly(projectTests(":analysis:low-level-api-fir"))
    testRuntimeOnly(projectTests(":analysis:analysis-api-impl-base"))
    testImplementation(projectTests(":analysis:analysis-api-fir"))
    testImplementation(projectTests(":analysis:analysis-test-framework"))
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:tests-common-new"))

    if (!kotlinBuildProperties.isInJpsBuildIdeaSync) {
        testApi(projectTests(":native:native.tests"))
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

val test by nativeTest("test", null)

publish()

runtimeJar()
sourcesJar()
javadocJar()

testsJar()

tasks.withType<KotlinJvmCompile> {
    compilerOptions.freeCompilerArgs.add("-Xcontext-parameters")
}

/**
 * Run swift-export-standalone tests against swift-export-embeddable and its runtime classpath:
 * - [shadedTestClassesJar] is a separate jar to avoid reshading on test classes changes
 * - [shadedIntransitiveTestDependenciesJar] are intransitive test dependencies that must be shaded. These must be intransitive and exclusive
 * to tests execution in order to avoid embedding dependencies not present in swift-export-embeddable's runtime classpath and hiding runtime
 * errors
 * -
 */
val shadedTestClassesJar = rewriteDepsToShadedJar(
    tasks.register<Jar>("testClassesJar") {
        from(sourceSets["test"].output.classesDirs)
        destinationDirectory.set(project.layout.buildDirectory.dir("testClasses"))
    },
    embeddableCompilerDummyForDependenciesRewriting("shadedTestClassesJar") {
        destinationDirectory.set(project.layout.buildDirectory.dir("testClassesShaded"))
    }
)

val shadedIntransitiveTestDependenciesJar = rewriteDepsToShadedJar(
    run {
        val testDependenciesClasspath = configurations.detachedConfiguration().apply {
            isTransitive = false
            dependencies.add(project.dependencies.create(commonDependency("commons-lang:commons-lang")))
            dependencies.add(project.dependencies.project(":native:executors"))
            dependencies.add(project.dependencies.project(":kotlin-compiler-runner-unshaded"))
            dependencies.add(project.dependencies.project(":kotlin-test"))

            dependencies.add(project.dependencies.projectTests(":native:native.tests"))
            dependencies.add(project.dependencies.projectTests(":compiler:tests-compiler-utils"))
            dependencies.add(project.dependencies.projectTests(":compiler:tests-common"))
            dependencies.add(project.dependencies.projectTests(":compiler:tests-common-new"))
            dependencies.add(project.dependencies.projectTests(":compiler:test-infrastructure"))
            dependencies.add(project.dependencies.projectTests(":compiler:test-infrastructure-utils"))
        }

        val testDependencies = layout.files(testDependenciesClasspath)
        val output = layout.buildDirectory.dir("unarchiveTestDependencies")
        val archiveOperations: ArchiveOperations = serviceOf<ArchiveOperations>()
        val fsOperations: FileSystemOperations = serviceOf<FileSystemOperations>()

        val unarchive = tasks.register("unarchiveTestDependencies") {
            inputs.files(testDependencies)
            outputs.dir(output)

            doLast {
                fsOperations.copy {
                    duplicatesStrategy = DuplicatesStrategy.WARN
                    testDependencies.forEach {
                        from(archiveOperations.zipTree(it).matching { exclude("META-INF/MANIFEST.MF") })
                    }
                    into(output)
                }
            }
        }

        tasks.register<Jar>("testDependencies") {
            from(unarchive)
            destinationDirectory.set(project.layout.buildDirectory.dir("testDependencies"))
        }
    },
    embeddableCompilerDummyForDependenciesRewriting("shadedTestDependencies") {
        destinationDirectory.set(project.layout.buildDirectory.dir("testDependenciesShaded"))
    }
)

val swiftExportEmbeddableRuntimeClasspath = configurations.detachedConfiguration().apply {
    dependencies.add(project.dependencies.project(":native:swift:swift-export-embeddable"))
}
val transitiveTestRuntimeClasspath = configurations.detachedConfiguration().apply {
    dependencies.add(libs.junit.jupiter.engine.get())
}

val testTask = projectTest(
    taskName = "testWithSwiftExportEmbeddable",
    jUnitMode = JUnitMode.JUnit5,
) {
    classpath = files(
        shadedTestClassesJar,
        swiftExportEmbeddableRuntimeClasspath,
        transitiveTestRuntimeClasspath,
        shadedIntransitiveTestDependenciesJar,
    )

    systemProperty(
        "kotlin.internal.native.test.nativeHome",
        project(":kotlin-native").projectDir.resolve("dist").absolutePath
    )
    systemProperty(
        "kotlin.internal.native.test.teamcity",
        kotlinBuildProperties.isTeamcityBuild.toString()
    )
    environment("GRADLE_TASK_NAME", path)

    val kotlinNativeCompilerEmbeddable = configurations.detachedConfiguration(
        dependencies.project(":kotlin-native:prepare:kotlin-native-compiler-embeddable"),
        dependencies.create(commonDependency("org.jetbrains.intellij.deps:trove4j"))
    ).also { dependsOn(it) }
    val kotlinNativeCompilerClasspath = provider { kotlinNativeCompilerEmbeddable.files.joinToString(File.pathSeparator) }
    doFirst {
        systemProperty(
            "kotlin.internal.native.test.compilerClasspath",
            kotlinNativeCompilerClasspath.get(),
        )
    }

    inputs.dir(projectDir.resolve("testData"))
    workingDir = rootDir

    useJUnitPlatform()
}