plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val compilerModules: Array<String> by rootProject.extra

val generateTests by generator("org.jetbrains.kotlin.jps.GenerateJpsPluginTestsKt") {
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(11))
        }
    )
}

repositories {
    // For rd-core, rd-framework and rd-swing
    maven("https://cache-redirector.jetbrains.com/www.myget.org/F/rd-snapshots/maven")
    maven("https://cache-redirector.jetbrains.com/www.myget.org/F/rd-model-snapshots/maven")
}

dependencies {
    api(project(":kotlin-build-common"))
    api(project(":core:descriptors"))
    api(project(":core:descriptors.jvm"))
    api(project(":kotlin-compiler-runner-unshaded"))
    api(project(":kotlin-compiler-runner"))
    api(project(":daemon-common"))
    api(project(":daemon-common-new"))
    api(project(":kotlin-daemon-client"))
    api(project(":kotlin-daemon"))
    testImplementation(projectTests(":generators:test-generator")) // TODO FIX ME
    testApi(projectTests(":generators:test-generator"))
    api(project(":compiler:frontend.java"))
    api(project(":js:js.frontend"))
    api(project(":kotlin-preloader"))
    api(project(":jps:jps-common"))
    compileOnly("org.jetbrains.intellij.deps:asm-all:9.1")
    compileOnly(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))
    compileOnly(intellijCore())
    compileOnly(jpsModel())
    compileOnly(jpsModelImpl())
    compileOnly(jpsBuild())
    compileOnly(jpsModelSerialization())
    testApi(jpsModel())
    testApi(testFramework())
    testCompileOnly(project(":kotlin-reflect-api"))
    testApi(project(":compiler:incremental-compilation-impl"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(projectTests(":compiler:incremental-compilation-impl"))
    testApi(commonDependency("junit:junit"))
    testApi(project(":kotlin-test:kotlin-test-jvm"))
    testApi(projectTests(":kotlin-build-common"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testCompileOnly(jpsBuild())
    testApi(devKitJps())

    testApi(intellijCore())

    testApi(jpsBuildTest())
    compilerModules.forEach {
        testRuntimeOnly(project(it))
    }

    testRuntimeOnly(toolsJar())
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":kotlin-script-runtime"))
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

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
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
