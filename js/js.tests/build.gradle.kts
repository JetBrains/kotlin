import com.moowork.gradle.node.exec.ExecRunner
import com.moowork.gradle.node.npm.NpmExecRunner
import com.moowork.gradle.node.npm.NpmTask

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("com.moowork.node").version("1.2.0")
}

node {
    download = true
}

val antLauncherJar by configurations.creating

dependencies {
    testRuntime(intellijDep())

    testCompile(protobufFull())
    testCompile(projectTests(":compiler:tests-common"))
    testCompileOnly(project(":compiler:frontend"))
    testCompileOnly(project(":compiler:cli"))
    testCompileOnly(project(":compiler:util"))
    testCompile(intellijCoreDep()) { includeJars("intellij-core") }
    testCompileOnly(intellijDep()) { includeJars("openapi", "idea", "idea_rt", "util") }
    testCompile(project(":compiler:backend.js"))
    testCompile(project(":js:js.translator"))
    testCompile(project(":js:js.serializer"))
    testCompile(project(":js:js.dce"))
    testCompile(commonDep("junit:junit"))
    testCompile(projectTests(":kotlin-build-common"))
    testCompile(projectTests(":generators:test-generator"))

    testRuntime(projectDist(":kotlin-stdlib"))
    testRuntime(projectDist(":kotlin-stdlib-js"))
    testRuntime(projectDist(":kotlin-test:kotlin-test-js")) // to be sure that kotlin-test-js built before tests runned
    testRuntime(projectDist(":kotlin-reflect"))
    testRuntime(projectDist(":kotlin-preloader")) // it's required for ant tests
    testRuntime(project(":compiler:backend-common"))
    testRuntime(commonDep("org.fusesource.jansi", "jansi"))

    antLauncherJar(commonDep("org.apache.ant", "ant"))
    antLauncherJar(files(toolsJar()))
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

val testDistProjects = listOf(
        "", // for root project
        ":kotlin-stdlib:jvm-minimal-for-test",
        ":kotlin-compiler",
        ":kotlin-script-runtime",
        ":kotlin-stdlib",
        ":kotlin-daemon-client",
        ":kotlin-ant")

projectTest {
    dependsOn(*testDistProjects.map { "$it:dist" }.toTypedArray())
    workingDir = rootDir
    doFirst {
        systemProperty("kotlin.ant.classpath", antLauncherJar.asPath)
        systemProperty("kotlin.ant.launcher.class", "org.apache.tools.ant.Main")
        systemProperty("idea.home.path", intellijRootDir().canonicalPath)
    }
}

testsJar {}

projectTest("quickTest") {
    dependsOn(*testDistProjects.map { "$it:dist" }.toTypedArray())
    workingDir = rootDir
    systemProperty("kotlin.js.skipMinificationTest", "true")
    doFirst {
        systemProperty("kotlin.ant.classpath", antLauncherJar.asPath)
        systemProperty("kotlin.ant.launcher.class", "org.apache.tools.ant.Main")
    }
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
