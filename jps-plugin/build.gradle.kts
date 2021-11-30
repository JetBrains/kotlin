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
    compileOnly("org.jetbrains.intellij.deps:asm-all:9.1")
    compileOnly(intellijDep()) {
        includeJars("jdom", "trove4j", "jps-model", "platform-api", "util", rootProject = rootProject)
    }
    compileOnly(jpsStandalone()) { includeJars("jps-builders", "jps-builders-6") }
    testCompileOnly(project(":kotlin-reflect-api"))
    testCompile(project(":compiler:incremental-compilation-impl"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(projectTests(":compiler:incremental-compilation-impl"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":kotlin-test:kotlin-test-jvm"))
    testCompile(projectTests(":kotlin-build-common"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testCompileOnly(jpsStandalone()) { includeJars("jps-builders", "jps-builders-6") }
    Ide.IJ {
        testCompile(intellijDep("devkit"))
    }

    testCompile(intellijDep())

    testCompile(jpsBuildTest())
    compilerModules.forEach {
        testRuntime(project(it))
    }

    testRuntimeOnly(intellijPluginDep("java"))

    testRuntimeOnly(toolsJar())
    testRuntime(project(":kotlin-reflect"))
    testRuntime(project(":kotlin-script-runtime"))
}

sourceSets {
    "main" {
        projectDefault()
        resources.srcDir("resources-en")
    }
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
    dependsOn(":kotlin-stdlib-js-ir:packFullRuntimeKLib")
    workingDir = rootDir
}

testsJar {}
