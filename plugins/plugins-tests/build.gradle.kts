
apply { plugin("kotlin") }

dependencies {
    testCompile(project(":compiler:util"))
    testCompile(project(":compiler:backend"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":compiler.tests-common"))
    testCompile(project(":android-extensions-compiler"))
    testCompile(project(":plugins:android-extensions-idea"))
    testCompile(project(":plugins:allopen-ide")) { isTransitive = false }
    testCompile(project(":kotlin-allopen-compiler-plugin"))
    testCompile(project(":plugins:noarg-ide")) { isTransitive = false }
    testCompile(project(":kotlin-noarg-compiler-plugin"))
    testCompile(project(":plugins:annotation-based-compiler-plugins-ide-support")) { isTransitive = false }
    testCompile(project(":plugins:sam-with-receiver-ide")) { isTransitive = false }
    testCompile(project(":kotlin-sam-with-receiver-compiler-plugin"))
    testCompile(project(":idea:idea-android")) { isTransitive = false }
    testCompile(project(":plugins:lint")) { isTransitive = false }
    testCompile(project(":plugins:uast-kotlin"))
    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testCompile(projectTests(":jps-plugin"))
    testCompileOnly(ideaSdkDeps("jps-builders"))
    testCompile(ideaSdkDeps("jps-build-test", subdir = "jps/test"))
    testCompile(ideaPluginDeps("*.jar", plugin = "android", subdir = "lib/jps"))
    testRuntime(project(":jps-plugin"))
    testRuntime(ideaSdkDeps("*.jar"))
    testRuntime(ideaPluginDeps("idea-junit", "resources_en", plugin = "junit"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "gradle"))
    testRuntime(ideaPluginDeps("*.jar", plugin = "android"))
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

testsJar {}

val test: Test by tasks
test.apply {
    dependsOnTaskIfExistsRec("dist", project = rootProject)
    workingDir = rootDir
    systemProperty("idea.is.unit.test", "true")
    environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
    ignoreFailures = true
}

