apply { plugin("kotlin") }

val compilerModules: Array<String> by rootProject.extra

dependencies {
    compile(project(":kotlin-build-common"))
    compile(project(":core"))
    compile(project(":kotlin-compiler-runner"))
    compile(project(":compiler:daemon-common"))
    compile(projectRuntimeJar(":kotlin-daemon-client"))
    compile(project(":compiler:frontend.java"))
    compile(projectRuntimeJar(":kotlin-preloader"))
    compile(project(":idea:idea-jps-common"))
    compile(ideaSdkDeps("jps-builders", /*"jps-builders-6",*/ subdir = "jps"))
    testCompile(project(":compiler.tests-common"))
    testCompile(project(":compiler:incremental-compilation-impl"))
    testCompile(projectTests(":compiler:incremental-compilation-impl"))
    testCompileOnly(ideaSdkDeps("jps-build-test", subdir = "jps/test"))
    testCompile(commonDep("junit:junit"))
    testCompile(projectDist(":kotlin-test:kotlin-test-jvm"))
    testCompile(projectTests(":kotlin-build-common"))
    compilerModules.forEach {
        testRuntime(project(it))
    }
    testRuntime(ideaSdkCoreDeps("*.jar"))
    testRuntime(ideaSdkDeps("*.jar"))
    testRuntime(ideaSdkDeps("*.jar", subdir = "jps/test"))
    testRuntime(ideaSdkDeps("*.jar", subdir = "jps"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        java.srcDirs("jps-tests/test"
                     /*, "kannotator-jps-plugin-test/test"*/ // Obsolete
        )
    }
}

projectTest {
    workingDir = rootDir
}

testsJar {}
