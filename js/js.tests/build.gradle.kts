import com.moowork.gradle.node.exec.ExecRunner
import com.moowork.gradle.node.npm.NpmExecRunner
import com.moowork.gradle.node.npm.NpmTask

plugins {
    id("com.moowork.node").version("1.2.0")
}

node {
    download = true
}

apply { plugin("kotlin") }

dependencies {
    testCompile(protobufFull())
    testCompile(projectTests(":compiler:tests-common"))
    testCompileOnly(project(":compiler:frontend"))
    testCompileOnly(project(":compiler:cli"))
    testCompileOnly(project(":compiler:util"))
    testCompile(project(":js:js.translator"))
    testCompile(project(":js:js.serializer"))
    testCompile(project(":js:js.dce"))
    testCompile(ideaSdkDeps("openapi", "idea", "idea_rt"))
    testCompile(commonDep("junit:junit"))
    testCompile(projectTests(":kotlin-build-common"))
    testCompile(projectTests(":generators:test-generator"))
    testRuntime(projectDist(":kotlin-compiler"))
    testRuntime(projectDist(":kotlin-stdlib"))
    testRuntime(projectDist(":kotlin-stdlib-js"))
    testRuntime(projectDist(":kotlin-test:kotlin-test-js")) // to be sure that kotlin-test-js built before tests runned
    testRuntime(projectDist(":kotlin-reflect"))
    testRuntime(projectDist(":kotlin-preloader")) // it's required for ant tests
    testRuntime(project(":compiler:backend-common"))
    testRuntime(commonDep("org.fusesource.jansi", "jansi"))
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

val testDistProjects = listOf(
        "", // for root project
        ":prepare:mock-runtime-for-test",
        ":kotlin-compiler",
        ":kotlin-script-runtime",
        ":kotlin-stdlib",
        ":kotlin-daemon-client",
        ":kotlin-ant")

projectTest {
    dependsOn(*testDistProjects.map { "$it:dist" }.toTypedArray())
    workingDir = rootDir
}

testsJar {}

projectTest("quickTest") {
    dependsOn(*testDistProjects.map { "$it:dist" }.toTypedArray())
    workingDir = rootDir
    systemProperty("kotlin.js.skipMinificationTest", "true")
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateJsTestsKt")
val testDataDir = project(":js:js.translator").projectDir.resolve("testData")

val install by task<NpmTask> {
    setWorkingDir(testDataDir)
    setArgs(listOf("install"))
}

val runMocha by task<NpmTask> {
    setWorkingDir(testDataDir)

    val target = if (project.hasProperty("teamcity")) "runOnTeamcity" else "test"
    setArgs(listOf("run", target))

    dependsOn(install, "test")

    val check by tasks
    check.dependsOn(this)
}
