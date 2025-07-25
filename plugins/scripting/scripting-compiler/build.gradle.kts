import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

description = "Kotlin Scripting Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("compiler-tests-convention")
}

val kotlinxSerializationGradlePluginClasspath by configurations.creating
val kotlinDataFrameGradlePluginClasspath by configurations.creating
val kotlinxCoroutinesCoreGradlePluginClasspath by configurations.creating

dependencies {
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:psi:psi-api"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:fir:entrypoint"))
    compileOnly(project(":compiler:fir:raw-fir:raw-fir.common"))
    compileOnly(project(":compiler:fir:tree"))
    compileOnly(project(":compiler:fir:plugin-utils"))
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
    testImplementation(project(":compiler:fir:plugin-utils"))
    testApi(testFixtures(project(":compiler:tests-common"))) { // TODO: drop this, it's based on JUnit4
        if (this is ProjectDependency) {
            exclude(group = "com.nordstrom.tools", module = "junit-foundation")
        }
    }
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testApi(libs.junit.platform.launcher)
    testApi(kotlinTest("junit5"))

    testApi(project(":kotlin-scripting-dependencies-maven"))

    testImplementation(intellijCore())
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    kotlinxSerializationGradlePluginClasspath(project(":kotlinx-serialization-compiler-plugin.embeddable")) { isTransitive = true }
    kotlinDataFrameGradlePluginClasspath(project(":kotlin-dataframe-compiler-plugin.embeddable")) { isTransitive = true }
    kotlinxCoroutinesCoreGradlePluginClasspath(libs.kotlinx.coroutines.core) { isTransitive = false }
}

optInToExperimentalCompilerApi()
optInToK1Deprecation()

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

compilerTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        dependsOn(":dist", kotlinxSerializationGradlePluginClasspath, kotlinDataFrameGradlePluginClasspath, kotlinxCoroutinesCoreGradlePluginClasspath)
        workingDir = rootDir
        val scriptClasspath = testSourceSet.output.classesDirs.joinToString(File.pathSeparator)
        val localKotlinxSerializationPluginClasspath: FileCollection = kotlinxSerializationGradlePluginClasspath
        val localKotlinDataFramePluginClasspath: FileCollection = kotlinDataFrameGradlePluginClasspath
        val localKotlinxCoroutinesCorePluginClasspath: FileCollection = kotlinxCoroutinesCoreGradlePluginClasspath
        doFirst {
            systemProperty("kotlin.test.script.classpath", scriptClasspath)
            systemProperty("kotlin.script.test.kotlinx.serialization.plugin.classpath", localKotlinxSerializationPluginClasspath.asPath)
            systemProperty("kotlin.script.test.kotlin.dataframe.plugin.classpath", localKotlinDataFramePluginClasspath.asPath)
            systemProperty("kotlin.script.test.kotlinx.coroutines.core.classpath", localKotlinxCoroutinesCorePluginClasspath.asPath)
        }
    }

    testTask("testWithK1", jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = false) {
        dependsOn(":dist")
        workingDir = rootDir
        val scriptClasspath = testSourceSet.output.classesDirs.joinToString(File.pathSeparator)

        doFirst {
            systemProperty("kotlin.test.script.classpath", scriptClasspath)
            systemProperty("kotlin.script.test.base.compiler.arguments", "-language-version 1.9")
            systemProperty("kotlin.script.base.compiler.arguments", "-language-version 1.9")
        }
    }
}
