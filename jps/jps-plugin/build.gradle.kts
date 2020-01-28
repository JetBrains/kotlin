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
    compile(project(":daemon-common"))
    compile(project(":daemon-common-new"))
    compile(projectRuntimeJar(":kotlin-daemon-client"))
    compile(projectRuntimeJar(":kotlin-daemon"))
    compile(project(":compiler:frontend.java"))
    compile(project(":js:js.frontend"))
    compile(projectRuntimeJar(":kotlin-preloader"))
    compile(project(":idea:idea-jps-common"))
    Platform[193].orLower {
        compileOnly(intellijDep()) { includeJars("openapi", rootProject = rootProject) }
    }
    compileOnly(intellijDep()) {
        includeJars("jdom", "trove4j", "jps-model", "platform-api", "util", "asm-all", rootProject = rootProject)
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

    testCompile(intellijDep())

    testCompile(jpsBuildTest())
    compilerModules.forEach {
        testRuntime(project(it))
    }

    Platform[192].orHigher {
        testRuntimeOnly(intellijPluginDep("java"))
    }

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

projectTest(parallel = true) {
    // do not replace with compile/runtime dependency,
    // because it forces Intellij reindexing after each compiler change
    dependsOn(":kotlin-compiler:dist")
    workingDir = rootDir
}

testsJar {}
