
apply { plugin("kotlin") }

configureIntellijPlugin {
    setPlugins("android")
}

dependencies {
    compile(project(":compiler:util"))
    compile(project(":jps-plugin"))
    compile(project(":plugins:android-extensions-compiler"))
    compile(ideaPluginDeps("android-jps-plugin", plugin = "android", subdir = "lib/jps"))

    testCompile(projectTests(":jps-plugin"))
    testCompile(project(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))
    testCompile(projectDist(":kotlin-test:kotlin-test-jvm"))
    testCompile(projectTests(":kotlin-build-common"))
}

afterEvaluate {
    dependencies {
        compileOnly(intellij { include("openapi.jar", "jps-builders.jar") })
        compile(intellijPlugin("android") { include("**/android-jps-plugin.jar") })
        testCompileOnly(intellijExtra("jps-build-test") { include("jps-build-test.jar") } )
        testRuntime(intellijPlugin("android"))
        testRuntime(intellijCoreJar())
        testRuntime(intellij())
        testRuntime(intellijExtra("jps-build-test"))
        testRuntime(intellijExtra("jps-standalone"))
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}

testsJar {}