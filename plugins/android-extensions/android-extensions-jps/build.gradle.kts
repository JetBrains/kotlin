
apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

dependencies {
    testRuntime(intellijDep())

    compile(project(":compiler:util"))
    compile(project(":jps-plugin"))
    compile(project(":plugins:android-extensions-compiler"))
    compileOnly(intellijDep()) { includeJars("openapi", "jps-builders") }
    compileOnly(intellijPluginDep("android")) { includeJars("jps/android-jps-plugin") }
    compile(intellijPluginDep("android")) { includeJars("jps/android-jps-plugin") }

    testCompile(projectTests(":jps-plugin"))
    testCompile(project(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))
    testCompile(projectDist(":kotlin-test:kotlin-test-jvm"))
    testCompile(projectTests(":kotlin-build-common"))
    testCompileOnly(intellijDep()) { includeJars("openapi", "jps-builders") }
    testCompileOnly(intellijDep("jps-build-test")) { includeJars("jps-build-test") }
    testCompileOnly(intellijDep()) { includeJars("jps-model") }

    testRuntime(intellijPluginDep("android"))
    testRuntime(intellijDep("jps-build-test"))
    testRuntime(intellijDep("jps-standalone"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
    useAndroidSdk()
    doFirst {
        systemProperty("idea.home.path", intellijRootDir().canonicalPath)
    }
}

testsJar {}