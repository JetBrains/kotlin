apply { plugin("kotlin") }

val compilerModules: Array<String> by rootProject.extra

dependencies {
    compileOnly(project(":jps-plugin:jps-services-declarations"))
    compile(project(":kotlin-build-common"))
    compile(project(":core:descriptors"))
    compile(project(":core:descriptors.jvm"))
    compile(project(":kotlin-compiler-runner"))
    compile(project(":compiler:daemon-common"))
    compile(projectRuntimeJar(":kotlin-daemon-client"))
    compile(project(":compiler:frontend.java"))
    compile(projectRuntimeJar(":kotlin-preloader"))
    compile(project(":idea:idea-jps-common"))
    compileOnly(intellijDep()) { includeJars("jdom", "trove4j", "jps-model", "openapi", "util", "asm-all") }
    compileOnly(intellijDep("jps-standalone")) { includeJars("jps-builders", "jps-builders-6") }
    testCompileOnly(project(":kotlin-reflect-api"))
    testCompile(project(":compiler:incremental-compilation-impl"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":compiler:incremental-compilation-impl"))
    testCompile(commonDep("junit:junit"))
    testCompile(projectDist(":kotlin-test:kotlin-test-jvm"))
    testCompile(projectTests(":kotlin-build-common"))
    testCompileOnly(intellijDep("jps-standalone")) { includeJars("jps-builders", "jps-builders-6") }
    testCompileOnly(intellijDep()) { includeJars("openapi", "idea", "log4j") }
    testCompile(intellijDep("jps-build-test"))
    compilerModules.forEach {
        testRuntime(project(it))
    }
    testRuntime(intellijDep())
    testRuntime(projectDist(":kotlin-reflect"))
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
    dependsOn(":kotlin-compiler:dist")
    workingDir = rootDir
}

testsJar {}
