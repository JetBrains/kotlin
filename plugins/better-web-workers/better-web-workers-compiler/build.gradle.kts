import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.d8.D8RootPlugin

description = "Better Web Workers compiler plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val testRuntime by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_RUNTIME))
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))

    compileOnly(project(":compiler:plugin-api")) // +
    compileOnly(project(":compiler:cli-common")) // аргументы cli
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:backend")) // +
    compileOnly(project(":compiler:ir.backend.common")) // +

    compileOnly(project(":js:js.frontend"))
    compileOnly(project(":js:js.translator"))
    compileOnly(project(":compiler:backend.js")) // +

    compileOnly(project(":compiler:backend.jvm"))
    compileOnly(project(":compiler:ir.tree")) // +

    compileOnly(kotlinStdlib())

    testApi(projectTests(":compiler:tests-common"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-compiler-utils"))
    testApi(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":generators:test-generator"))

    testImplementation(projectTests(":js:js.tests"))
    testApi(commonDependency("junit:junit"))
    testApi(project(":kotlin-test:kotlin-test-jvm"))

    testRuntimeOnly(kotlinStdlib())
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":kotlin-preloader")) // it's required for ant tests
    testRuntimeOnly(project(":compiler:backend-common"))
    testRuntimeOnly(commonDependency("org.fusesource.jansi", "jansi"))

    testRuntime("kotlinx.webworkers:kotlinx-webworkers-annotations:1.2-SNAPSHOT") { isTransitive = false }
    testRuntime("org.jetbrains.kotlinx:atomicfu-js:0.16.1") { isTransitive = false }
    testRuntime("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.5.0") { isTransitive = false }
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

runtimeJar()
sourcesJar()
javadocJar()
testsJar()

projectTest(jUnitMode = JUnitMode.JUnit5) {
    useJUnitPlatform()
    workingDir = rootDir
    dependsOn(testRuntime)
    doFirst {
        systemProperty("testRuntime.classpath", testRuntime.asPath)
    }
    setUpJsBoxTests()
}

val d8Plugin = D8RootPlugin.apply(rootProject)
d8Plugin.version = v8Version

fun Test.setupV8() {
    dependsOn(d8Plugin.setupTaskProvider)
    doFirst {
        systemProperty("javascript.engine.path.V8", d8Plugin.requireConfigured().executablePath.absolutePath)
    }
}

fun Test.setUpJsBoxTests() {
    setupV8()
    dependsOn(":dist")
    dependsOn(":kotlin-stdlib-js-ir:compileKotlinJs")
    systemProperty("kotlin.js.full.stdlib.path", "libraries/stdlib/js-ir/build/classes/kotlin/js/main")
    dependsOn(":kotlin-stdlib-js-ir-minimal-for-test:compileKotlinJs")
    systemProperty("kotlin.js.reduced.stdlib.path", "libraries/stdlib/js-ir-minimal-for-test/build/classes/kotlin/js/main")
    dependsOn(":kotlin-test:kotlin-test-js-ir:compileKotlinJs")
    systemProperty("kotlin.js.kotlin.test.path", "libraries/kotlin.test/js-ir/build/classes/kotlin/js/main")
    systemProperty("kotlin.js.kotlin.test.path", "libraries/kotlin.test/js-ir/build/classes/kotlin/js/main")
    systemProperty("kotlin.js.test.root.out.dir", "$buildDir/")
}
