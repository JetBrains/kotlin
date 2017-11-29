
apply { plugin("kotlin") }

dependencies {
    testCompile(project(":compiler:util"))
    testCompile(project(":compiler:backend"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":plugins:android-extensions-compiler"))
    testCompile(project(":plugins:android-extensions-ide"))
    testCompile(project(":kotlin-android-extensions-runtime"))
    testCompile(project(":allopen-ide-plugin")) { isTransitive = false }
    testCompile(project(":kotlin-allopen-compiler-plugin"))
    testCompile(project(":noarg-ide-plugin")) { isTransitive = false }
    testCompile(project(":kotlin-noarg-compiler-plugin"))
    testCompile(project(":plugins:annotation-based-compiler-plugins-ide-support")) { isTransitive = false }
    testCompile(project(":sam-with-receiver-ide-plugin")) { isTransitive = false }
    testCompile(project(":kotlin-sam-with-receiver-compiler-plugin"))
    testCompile(project(":idea:idea-android")) { isTransitive = false }
    testCompile(project(":plugins:lint")) { isTransitive = false }
    testCompile(project(":plugins:uast-kotlin"))
    testCompile(projectDist(":kotlin-test:kotlin-test-jvm"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":jps-plugin"))
    testCompile(commonDep("junit:junit"))
    testCompileOnly(ideaSdkDeps("jps-builders"))
    testCompile(ideaSdkDeps("jps-build-test", subdir = "jps/test"))
    testCompile(ideaPluginDeps("*.jar", plugin = "android", subdir = "lib/jps"))
    testRuntime(projectDist(":kotlin-reflect"))
    testRuntime(project(":jps-plugin"))
    testRuntime(projectTests(":compiler:tests-common-jvm6"))
    testRuntime(ideaSdkDeps("*.jar"))
    testRuntime(ideaPluginDeps("idea-junit", "resources_en", plugin = "junit"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "gradle"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "android"))
    testRuntime(preloadedDeps("dx", subdir = "android-5.0/lib"))
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

testsJar {}

evaluationDependsOn(":kotlin-android-extensions-runtime")

projectTest {
    environment("ANDROID_EXTENSIONS_RUNTIME_CLASSES", getSourceSetsFrom(":kotlin-android-extensions-runtime")["main"].output.classesDirs.asPath)
    dependsOnTaskIfExistsRec("dist", project = rootProject)
    workingDir = rootDir
}
