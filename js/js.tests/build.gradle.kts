
apply { plugin("kotlin") }

dependencies {
    testCompile(project(":compiler.tests-common"))
    testCompile(project(":compiler:frontend"))
    testCompile(project(":compiler:cli"))
    testCompileOnly(project(":compiler:util"))
    testCompile(project(":js:js.translator"))
    testCompile(project(":js:js.serializer"))
    testCompile(project(":js:js.dce"))
    testCompile(ideaSdkDeps("openapi", "idea"))
    testRuntime(projectDist(":kotlin-stdlib"))
    testRuntime(project(":compiler:backend-common"))
    testRuntime(ideaSdkDeps("*.jar"))
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
        ":kotlin-runtime",
        ":kotlin-script-runtime",
        ":kotlin-stdlib",
        ":kotlin-stdlib-js",
        ":kotlin-test:kotlin-test-jvm",
        ":kotlin-test:kotlin-test-junit",
        ":kotlin-daemon-client",
        ":kotlin-ant")

projectTest {
    dependsOn(*testDistProjects.map { "$it:dist" }.toTypedArray())
    workingDir = rootDir
}

testsJar {}

