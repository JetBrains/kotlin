plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val compilerModules: Array<String> by rootProject.extra

dependencies {
    compile(project(":kotlin-build-common"))
    compile(project(":core:descriptors"))
    compile(project(":core:descriptors.jvm"))
    compile(project(":kotlin-compiler-runner"))
    compile(project(":compiler:daemon-common"))
    compile(projectRuntimeJar(":kotlin-daemon-client"))
    compile(project(":compiler:frontend.java"))
    compile(project(":js:js.frontend"))
    compile(projectRuntimeJar(":kotlin-preloader"))
    compile(project(":idea:idea-jps-common"))
    compileOnly(intellijDep()) {
        if (Platform[181].orHigher()) {
            includeJars("jdom", "trove4j", "jps-model", "openapi", "platform-api", "util", "asm-all", rootProject = rootProject)
        } else {
            includeJars("jdom", "trove4j", "jps-model", "openapi", "util", "asm-all", rootProject = rootProject)
        }
    }
    compileOnly(jpsStandalone()) { includeJars("jps-builders", "jps-builders-6") }
    testCompileOnly(project(":kotlin-reflect-api"))
    testCompile(project(":compiler:incremental-compilation-impl"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":compiler:incremental-compilation-impl"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testCompile(projectTests(":kotlin-build-common"))
    testCompileOnly(jpsStandalone()) { includeJars("jps-builders", "jps-builders-6") }
    Ide.IJ {
        testCompile(intellijDep("devkit"))
    }
    if (Platform[181].orHigher()) {
        testCompileOnly(intellijDep()) { includeJars("openapi", "idea", "platform-api", "log4j") }
    } else {
        testCompileOnly(intellijDep()) { includeJars("openapi", "idea", "log4j") }
    }
    testCompile(jpsBuildTest())
    compilerModules.forEach {
        testRuntime(project(it))
    }
    testRuntime(intellijDep())
    testRuntime(project(":kotlin-reflect"))
    testRuntime(project(":kotlin-script-runtime"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        Ide.IJ {
            java.srcDirs("jps-tests/test")
        }
    }
}

projectTest {
    // do not replace with compile/runtime dependency,
    // because it forces Intellij reindexing after each compiler change
    dependsOn(":kotlin-compiler:dist")
    workingDir = rootDir
}

testsJar {}
