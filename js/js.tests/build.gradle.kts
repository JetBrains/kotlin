
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
    testRuntime(project(":kotlin-stdlib"))
    testRuntime(project(":compiler:backend-common"))
    testRuntime(ideaSdkDeps("*.jar"))
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

val test: Test by tasks
test.apply {
    dependsOnTaskIfExistsRec("dist", project = rootProject)
    dependsOn(":prepare:mock-runtime-for-test:dist")
    workingDir = rootDir
    systemProperty("idea.is.unit.test", "true")
    environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
    ignoreFailures = true
}

testsJar {}

