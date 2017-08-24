
apply { plugin("kotlin") }

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":core:util.runtime"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:light-classes"))
    compileOnly(ideaSdkDeps("openapi", "idea"))
    testCompile(projectDist(":kotlin-test:kotlin-test-jvm"))
    testCompile(project(":compiler.tests-common"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":compiler:util"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":idea:idea-android"))
    testCompile(preloadedDeps("uast-tests"))
    testRuntime(ideaSdkDeps("*.jar"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar {}

projectTest {
    workingDir = rootDir
}
