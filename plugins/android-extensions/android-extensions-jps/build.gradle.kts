
apply { plugin("kotlin") }

val androidSdk by configurations.creating

dependencies {
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

    testRuntime(intellijPluginDep("android"))
    testRuntime(intellijCoreDep()) { includeJars("intellij-core") }
    testRuntime(intellijDep())
    testRuntime(intellijDep("jps-build-test"))
    testRuntime(intellijDep("jps-standalone"))

    androidSdk(project(":custom-dependencies:android-sdk", configuration = "androidSdk"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
    doFirst {
        systemProperty("android.sdk", androidSdk.singleFile.canonicalPath)
    }
}

testsJar {}