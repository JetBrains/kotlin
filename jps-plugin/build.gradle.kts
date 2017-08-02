apply {
    plugin("kotlin")
}

dependencies {
    val compile by configurations
    val compileOnly by configurations
    val testCompile by configurations
    val testCompileOnly by configurations
    val testRuntime by configurations
    testRuntime(ideaSdkCoreDeps("*.jar"))
    testRuntime(ideaSdkDeps("*.jar"))
    testRuntime(ideaSdkDeps("*.jar", subdir = "jps/test"))
    testRuntime(ideaSdkDeps("*.jar", subdir = "jps"))
    compile(project(":build-common"))
    compile(project(":core"))
    compile(project(":compiler:compiler-runner"))
    compile(project(":compiler:daemon-common"))
    compile(project(":compiler:daemon-client"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:preloader"))
    compile(project(":idea:idea-jps-common"))
    compile(ideaSdkDeps("jps-builders", "jps-builders-6", subdir = "jps"))
    buildVersion()
    testCompile(project(":compiler.tests-common"))
    testCompile(project(":compiler:incremental-compilation-impl"))
    testCompileOnly(ideaSdkDeps("jps-build-test", subdir = "jps/test"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testCompile(projectTests(":build-common"))
    (rootProject.extra["compilerModules"] as Array<String>).forEach {
        testRuntime(project(it))
    }
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectResourcesDefault()
configureKotlinProjectTests("test", sourcesBaseDir = File(projectDir, "jps-tests"))
configureKotlinProjectTestResources("testData")


tasks.withType<Test> {
    jvmArgs("-ea", "-XX:+HeapDumpOnOutOfMemoryError", "-Xmx1200m", "-XX:+UseCodeCacheFlushing", "-XX:ReservedCodeCacheSize=128m", "-Djna.nosys=true")
    maxHeapSize = "1200m"
    workingDir = rootDir
    systemProperty("idea.is.unit.test", "true")
    forkEvery = 100
    ignoreFailures = true
}

testsJar {}
