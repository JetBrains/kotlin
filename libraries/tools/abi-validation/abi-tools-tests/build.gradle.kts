import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
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
val testFatJarSourceSet = sourceSets.create("testFatJar") {
    kotlin.srcDirs(sharedTestsSourceSet.kotlin.srcDirs)
}


// Inherit runtime configuration from the conventional `test` to get common runtime-only deps
// we copy dependencies from the `test` source set because some of them can be added in common configurations
configurations.getByName("sharedTestsRuntimeOnly").extendsFrom(configurations.getByName("testRuntimeOnly"))
configurations.getByName("sharedTestsImplementation").extendsFrom(configurations.getByName("testImplementation"))

configurations.getByName("testOriginalRuntimeOnly").extendsFrom(configurations.getByName("testRuntimeOnly"))
configurations.getByName("testOriginalImplementation").extendsFrom(configurations.getByName("testImplementation"))

configurations.getByName("testFatJarRuntimeOnly").extendsFrom(configurations.getByName("testRuntimeOnly"))
configurations.getByName("testFatJarImplementation").extendsFrom(configurations.getByName("testImplementation"))


dependencies {
    // common dependencies for all tests
    testImplementation(kotlinStdlib())
    testImplementation(kotlinTest("junit"))

    "compilingImplementation"(kotlinStdlib())

    "sharedTestsCompileOnly"(project(":libraries:tools:abi-validation:abi-tools-api"))

    "testOriginalImplementation"(project(":libraries:tools:abi-validation:abi-tools"))
    "testFatJarImplementation"(project(":libraries:tools:abi-validation:abi-tools-dep"))
}

tasks.named<KotlinCompile>("compileCompilingKotlin") {
    compilerOptions.freeCompilerArgs.add("-jvm-default=enable")
}

// don't use `test` task
tasks.test {
    enabled = false
}

projectTests {
    testTask(taskName = "testOriginal", skipInLocalBuild = false, jUnitMode = JUnitMode.JUnit4) {
        group = "verification"
        testClassesDirs = testOriginalSourceSet.output.classesDirs
        classpath = testOriginalSourceSet.runtimeClasspath

        useJUnit()
        systemProperty("overwrite.output", System.getProperty("overwrite.output", "false"))
        systemProperty("testCasesClassesDirs", compilingSourceSet.output.classesDirs.asPath)

        dependsOn(compilingSourceSet.output)
    }

    testTask(taskName = "testFatJar", skipInLocalBuild = false, jUnitMode = JUnitMode.JUnit4) {
        group = "verification"
        testClassesDirs = testFatJarSourceSet.output.classesDirs
        classpath = testFatJarSourceSet.runtimeClasspath

        useJUnit()
        systemProperty("overwrite.output", System.getProperty("overwrite.output", "false"))
        systemProperty("testCasesClassesDirs", compilingSourceSet.output.classesDirs.asPath)

        dependsOn(compilingSourceSet.output)
    }
}

tasks.check {
    dependsOn("testOriginal")
    dependsOn("testFatJar")
}
