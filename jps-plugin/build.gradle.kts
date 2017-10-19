apply { plugin("kotlin") }

val compilerModules: Array<String> by rootProject.extra

dependencies {
    compile(project(":kotlin-build-common"))
    compile(project(":core:descriptors"))
    compile(project(":core:descriptors.jvm"))
    compile(project(":kotlin-compiler-runner"))
    compile(project(":compiler:daemon-common"))
    compile(projectRuntimeJar(":kotlin-daemon-client"))
    compile(project(":compiler:frontend.java"))
    compile(projectRuntimeJar(":kotlin-preloader"))
    compile(project(":idea:idea-jps-common"))
    compile(ideaSdkDeps("jps-builders", "jps-builders-6", subdir = "jps"))
    compile(ideaSdkDeps("jps-model.jar", subdir = "jps"))
    testCompileOnly(project(":kotlin-reflect-api"))
    testCompile(project(":compiler:incremental-compilation-impl"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":compiler:incremental-compilation-impl"))
    testCompile(ideaSdkDeps("openapi", "idea"))
    //testCompileOnly(ideaSdkDeps("jps-build-test", subdir = "jps/test"))
    testCompile(commonDep("junit:junit"))
    testCompile(projectDist(":kotlin-test:kotlin-test-jvm"))
    testCompile(projectTests(":kotlin-build-common"))
    compilerModules.forEach {
        testRuntime(project(it))
    }
    testRuntime(projectDist(":kotlin-reflect"))
    testRuntime(ideaSdkCoreDeps("*.jar"))
    testRuntime(ideaSdkDeps("resources"))
    testRuntime(ideaSdkDeps("*.jar"))
    //testRuntime(ideaSdkDeps("*.jar", subdir = "jps/test"))
    testRuntime(ideaSdkDeps("*.jar", subdir = "jps"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        /*java.srcDirs("jps-tests/test"
                     /*, "kannotator-jps-plugin-test/test"*/ // Obsolete
        )*/
    }
}

projectTest {
    dependsOn(":kotlin-compiler:dist")
    workingDir = rootDir
}

testsJar {}
