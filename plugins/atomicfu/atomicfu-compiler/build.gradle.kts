import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages

description = "Atomicfu Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("com.github.node-gradle.node") version "2.2.0"
    id("de.undercouch.download")
}

node {
    download = true
    version = "10.16.2"
}

val antLauncherJar by configurations.creating
val testJsRuntime by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_RUNTIME))
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
    }
}

val atomicfuClasspath by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
    }
}

val atomicfuRuntimeForTests by configurations.creating {
    attributes {
        attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
        attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_RUNTIME))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "asm-all", rootProject = rootProject) }

    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":js:js.frontend"))
    compileOnly(project(":js:js.translator"))
    compileOnly(project(":compiler:backend.js"))

    compileOnly(kotlinStdlib())

    testApi(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":js:js.tests"))
    testApi(commonDep("junit:junit"))

    testRuntimeOnly(kotlinStdlib())
    testRuntimeOnly(project(":kotlin-reflect"))
    testRuntimeOnly(project(":kotlin-preloader")) // it's required for ant tests
    testRuntimeOnly(project(":compiler:backend-common"))
    testRuntimeOnly(commonDep("org.fusesource.jansi", "jansi"))

    atomicfuClasspath("org.jetbrains.kotlinx:atomicfu-js:0.16.3") { isTransitive = false }
    atomicfuRuntimeForTests(project(":kotlinx-atomicfu-runtime"))  { isTransitive = false }

    embedded(project(":kotlinx-atomicfu-runtime")) {
        attributes {
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.js)
            attribute(KotlinJsCompilerAttribute.jsCompilerAttribute, KotlinJsCompilerAttribute.ir)
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_RUNTIME))
        }
        isTransitive = false
    }

    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.6.2")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar()
sourcesJar()
javadocJar()
testsJar()

projectTest {
    workingDir = rootDir
    dependsOn(atomicfuRuntimeForTests)
    doFirst {
        systemProperty("atomicfuRuntimeForTests.classpath", atomicfuRuntimeForTests.asPath)
        systemProperty("atomicfu.classpath", atomicfuClasspath.asPath)
    }
    setUpJsIrBoxTests()
}

fun Test.setupV8() {
    dependsOn(":js:js.tests:unzipV8")
    doFirst {
        val unzipV8Task = project.tasks.getByPath(":js:js.tests:unzipV8")
        systemProperty("javascript.engine.path.V8", File(unzipV8Task.outputs.files.single().path, "d8"))
    }
}

fun Test.setUpJsIrBoxTests() {
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