
apply { plugin("kotlin") }

dependencies {
    testCompile(protobufFull())
    testCompile(project(":compiler:tests-common"))
    testCompileOnly(project(":compiler:frontend"))
    testCompileOnly(project(":compiler:cli"))
    testCompileOnly(project(":compiler:util"))
    testCompile(project(":js:js.translator"))
    testCompile(project(":js:js.serializer"))
    testCompile(project(":js:js.dce"))
    testCompile(ideaSdkDeps("openapi", "idea"))
    testRuntime(commonDep("junit:junit"))
    testRuntime(projectDist(":kotlin-stdlib"))
    testRuntime(projectDist(":kotlin-stdlib-js"))
    testRuntime(projectDist(":kotlin-test:kotlin-test-js")) // to be sure that kotlin-test-js built before tests runned
    testRuntime(projectDist(":kotlin-reflect"))
    testRuntime(projectDist(":kotlin-preloader")) // it's required for ant tests
    testRuntime(project(":compiler:backend-common"))
//    testRuntime(ideaSdkDeps("*.jar"))
    testRuntime(commonDep("org.fusesource.jansi", "jansi"))
    testCompile(projectTests(":kotlin-build-common"))
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
