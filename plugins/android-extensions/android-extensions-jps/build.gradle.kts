
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testRuntime(intellijDep())

    compile(project(":compiler:util"))
    compile(project(":jps-plugin"))
    compile(project(":plugins:android-extensions-compiler"))
    compileOnly(intellijDep()) { includeJars("openapi", "jps-builders", "jps-model", "jdom") }
    Platform[181].orHigher {
        compileOnly(intellijDep()) { includeJars("platform-api") }
    }
    compileOnly(intellijPluginDep("android")) { includeJars("jps/android-jps-plugin") }
    compile(intellijPluginDep("android")) { includeJars("jps/android-jps-plugin") }

    testCompile(projectTests(":jps-plugin"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testCompile(projectTests(":kotlin-build-common"))
    testCompileOnly(intellijDep()) { includeJars("openapi", "jps-builders") }
    testCompileOnly(intellijDep("jps-build-test")) { includeJars("jps-build-test") }
    testCompileOnly(intellijDep()) { includeJars("jps-model") }

    testRuntime(intellijPluginDep("android"))
    (Platform[181].orHigher.or(Ide.AS31)) {
        testRuntime(intellijPluginDep("smali"))
    }
    testRuntime(intellijDep("jps-build-test"))
    testRuntime(intellijDep("jps-standalone"))
}

sourceSets {
    Ide.IJ {
        "main" { projectDefault() }
        "test" { projectDefault() }
    }

    Ide.AS {
        "main" {}
        "test" {}
    }
}

projectTest {
    workingDir = rootDir
    useAndroidSdk()
}

testsJar {}