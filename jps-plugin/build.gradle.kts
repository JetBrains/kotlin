apply { plugin("kotlin") }

val compilerModules: Array<String> by rootProject.extra

configureIntellijPlugin {
    setExtraDependencies("intellij-core", "jps-standalone", "jps-build-test")
}

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
    testCompileOnly(project(":kotlin-reflect-api"))
    testCompile(project(":compiler:incremental-compilation-impl"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":compiler:incremental-compilation-impl"))
    testCompile(commonDep("junit:junit"))
    testCompile(projectDist(":kotlin-test:kotlin-test-jvm"))
    testCompile(projectTests(":kotlin-build-common"))
    compilerModules.forEach {
        testRuntime(project(it))
    }
    testRuntime(projectDist(":kotlin-reflect"))
}

afterEvaluate {
    dependencies {
        compileOnly(intellij { include("jdom.jar", "trove4j.jar", "jps-model.jar", "openapi.jar", "util.jar") })
        compileOnly(intellijExtra("jps-standalone") { include("jps-builders.jar", "jps-builders-6.jar") })
        testCompileOnly(intellijExtra("jps-standalone") { include("jps-builders.jar", "jps-builders-6.jar") })
        testCompileOnly(intellij { include("openapi.jar", "idea.jar", "log4j.jar") })
        testCompile(intellijExtra("jps-build-test"))
        testRuntime(intellij())
        testRuntime(intellijCoreJar())
    }
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
