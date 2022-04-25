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
    compileOnly(project(":jps:jps-platform-api-signatures"))
    testImplementation(projectTests(":generators:test-generator"))
    api(project(":compiler:frontend.java"))
    api(project(":js:js.frontend"))
    api(project(":kotlin-preloader"))
    api(project(":jps:jps-common"))
    compileOnly(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))
    compileOnly(jpsModel())
    compileOnly(jpsModelImpl())
    compileOnly(jpsBuild())
    compileOnly(jpsModelSerialization())
    testApi(jpsModel())

    // testFramework includes too many unnecessary dependencies. Here we manually list all we need to successfully run JPS tests
    testApi(testFramework()) { isTransitive = false }
    testApi("com.jetbrains.intellij.platform:test-framework-core:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:analysis-impl:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:boot:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:analysis:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:project-model:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:object-serializer:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:code-style:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:ide-impl:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:ide:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:util-ui:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:concurrency:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:editor:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:core-ui:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:lang:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:lang-impl:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:util-ex:$intellijVersion") { isTransitive = false }

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

    testApi(jpsBuildTest())
    compilerModules.forEach {
        testRuntimeOnly(project(it))
    }

    testRuntimeOnly(toolsJar())
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":kotlin-script-runtime"))
    testImplementation("org.projectlombok:lombok:1.18.16")
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

tasks.compileJava {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = "1.8"
}

projectTest(parallel = true) {
    // do not replace with compile/runtime dependency,
    // because it forces Intellij reindexing after each compiler change
    dependsOn(":kotlin-compiler:dist")
    dependsOn(":kotlin-stdlib-js-ir:packFullRuntimeKLib")
    workingDir = rootDir
}

testsJar {}
