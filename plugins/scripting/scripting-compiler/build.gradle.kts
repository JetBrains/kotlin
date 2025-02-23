import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

description = "Kotlin Scripting Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:psi"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:fir:entrypoint"))
    compileOnly(project(":compiler:fir:fir2ir"))
    compileOnly(project(":compiler:fir:java"))
    compileOnly(project(":compiler:fir:raw-fir:raw-fir.common"))
    compileOnly(project(":compiler:fir:resolve"))
    compileOnly(project(":compiler:fir:tree"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":core:descriptors.runtime"))
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(project(":compiler:backend.jvm.entrypoint"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    api(project(":kotlin-scripting-common"))
    api(project(":kotlin-scripting-jvm"))
    api(project(":kotlin-scripting-compiler-impl"))
    api(kotlinStdlib())
    api(commonDependency("org.jline", "jline"))
    compileOnly(intellijCore())

    implementation(project(":kotlin-power-assert-compiler-plugin")) // TODO: KT-74787

    testApi(project(":compiler:frontend"))
    testApi(project(":compiler:plugin-api"))
    testApi(project(":compiler:util"))
    testApi(project(":compiler:cli"))
    testApi(project(":compiler:cli-common"))
    testApi(project(":compiler:frontend.java"))
    testApi(projectTests(":compiler:tests-common")) // TODO: drop this, it's based on JUnit4
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testApi(libs.junit.platform.launcher)
    testApi(kotlinTest("junit5"))

    testImplementation(intellijCore())
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        progressiveMode.set(false)
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

publish()

runtimeJar()
sourcesJar()
javadocJar()

testsJar()

projectTest(parallel = true, jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
    val scriptClasspath = testSourceSet.output.classesDirs.joinToString(File.pathSeparator)
    doFirst {
        systemProperty("kotlin.test.script.classpath", scriptClasspath)
    }
}

projectTest(taskName = "testWithK1", parallel = true, jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
    val scriptClasspath = testSourceSet.output.classesDirs.joinToString(File.pathSeparator)

    doFirst {
        systemProperty("kotlin.test.script.classpath", scriptClasspath)
        systemProperty("kotlin.script.test.base.compiler.arguments", "-language-version 1.9")
        systemProperty("kotlin.script.base.compiler.arguments", "-language-version 1.9")
    }
}

