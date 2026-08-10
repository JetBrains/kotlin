import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("project-tests-convention")
}

// source set for test cases
val compilingSourceSet = sourceSets.create("compiling") {
    java.srcDir("src/compiling/kotlin")
}

// unrunnable tests - shared code for different ABI tools implementations
val sharedTestsSourceSet = sourceSets.create("sharedTests")

// source sets for runnable tests
val testOriginalSourceSet = sourceSets.create("testOriginal") {
    kotlin.srcDirs(sharedTestsSourceSet.kotlin.srcDirs)
}
val testEmbeddableSourceSet = sourceSets.create("testEmbeddable") {
    kotlin.srcDirs(sharedTestsSourceSet.kotlin.srcDirs)
}


// Inherit runtime configuration from the conventional `test` to get common runtime-only deps
// we copy dependencies from the `test` source set because some of them can be added in common configurations
configurations.getByName("sharedTestsRuntimeOnly").extendsFrom(configurations.testRuntimeOnly.get())
configurations.getByName("sharedTestsImplementation").extendsFrom(configurations.testImplementation.get())

configurations.getByName("testOriginalRuntimeOnly").extendsFrom(configurations.testRuntimeOnly.get())
configurations.getByName("testOriginalImplementation").extendsFrom(configurations.testImplementation.get())

configurations.getByName("testEmbeddableRuntimeOnly").extendsFrom(configurations.testRuntimeOnly.get())
configurations.getByName("testEmbeddableImplementation").extendsFrom(configurations.testImplementation.get())


dependencies {
    // common dependencies for all tests
    testImplementation(kotlinStdlib())
    testImplementation(kotlinTest("junit5"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    "compilingImplementation"(kotlinStdlib())

    "sharedTestsCompileOnly"(project(":libraries:tools:abi-validation:abi-tools-api"))

    "testOriginalImplementation"(project(":libraries:tools:abi-validation:abi-tools"))
    "testEmbeddableImplementation"(project(":libraries:tools:abi-validation:abi-tools-embeddable"))
}

tasks.named<KotlinCompile>("compileCompilingKotlin") {
    compilerOptions.freeCompilerArgs.add("-jvm-default=enable")
}

// don't use `test` task
tasks.test {
    enabled = false
}

projectTests {
    testTask(taskName = "testOriginal", skipInLocalBuild = false) {
        group = "verification"
        testClassesDirs = testOriginalSourceSet.output.classesDirs
        classpath = testOriginalSourceSet.runtimeClasspath

        systemProperty("overwrite.output", System.getProperty("overwrite.output", "false"))
        systemProperty("testCasesClassesDirs", compilingSourceSet.output.classesDirs.asPath)

        dependsOn(compilingSourceSet.output)
    }

    testTask(taskName = "testEmbeddable", skipInLocalBuild = false) {
        group = "verification"
        testClassesDirs = testEmbeddableSourceSet.output.classesDirs
        classpath = testEmbeddableSourceSet.runtimeClasspath

        systemProperty("overwrite.output", System.getProperty("overwrite.output", "false"))
        systemProperty("testCasesClassesDirs", compilingSourceSet.output.classesDirs.asPath)

        dependsOn(compilingSourceSet.output)
    }
}

tasks.check {
    dependsOn("testOriginal")
    dependsOn("testEmbeddable")
}
