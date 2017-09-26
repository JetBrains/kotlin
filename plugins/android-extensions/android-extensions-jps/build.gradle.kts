
apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:util"))
    compile(project(":jps-plugin"))
    compile(project(":plugins:android-extensions-compiler"))
    compile(ideaPluginDeps("android-jps-plugin", plugin = "android", subdir = "lib/jps"))

    testCompile(projectTests(":jps-plugin"))
    testCompile(project(":compiler:tests-common"))
    // testCompileOnly(ideaSdkDeps("jps-build-test", subdir = "jps/test"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "android"))
    testCompile(commonDep("junit:junit"))
    testCompile(projectDist(":kotlin-test:kotlin-test-jvm"))
    testCompile(projectTests(":kotlin-build-common"))
    testRuntime(ideaSdkCoreDeps("*.jar"))
    testRuntime(ideaSdkDeps("*.jar"))
    // testRuntime(ideaSdkDeps("*.jar", subdir = "jps/test"))
    testRuntime(ideaSdkDeps("*.jar", subdir = "jps"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}

testsJar {}