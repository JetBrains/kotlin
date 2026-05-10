import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

description = "Kotlin Scripting Compiler Plugin"

plugins {
    kotlin("jvm")
    id("project-tests-convention")
}

val kotlinxSerializationGradlePluginClasspath by configurations.creating
val kotlinDataFrameGradlePluginClasspath by configurations.creating
val kotlinxCoroutinesCoreGradlePluginClasspath by configurations.creating
val kotlinAllOpenPluginJar by configurations.creating
val kotlinMainKtsPluginJar by configurations.creating
val kotlinScriptingCommonJar by configurations.creating
val powerAssertCompilerPluginJar by configurations.creating

dependencies {
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:psi:psi-api"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:fir:entrypoint"))
    compileOnly(project(":compiler:fir:raw-fir:raw-fir.common"))
    compileOnly(project(":compiler:fir:tree"))
    compileOnly(project(":compiler:fir:providers"))
    compileOnly(project(":compiler:fir:fir2ir:jvm-backend"))
    compileOnly(project(":compiler:fir:plugin-utils"))
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:cli-jvm"))
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
    implementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    testImplementation(project(":compiler:frontend"))
    testImplementation(project(":compiler:plugin-api"))
    testImplementation(project(":compiler:util"))
    testImplementation(project(":compiler:cli"))
    testImplementation(project(":compiler:frontend.java"))
    testImplementation(project(":compiler:fir:plugin-utils"))
    testImplementation(testFixtures(project(":compiler:tests-common"))) { // TODO: drop this, it's based on JUnit4
        if (this is ProjectDependency) {
            exclude(group = "com.nordstrom.tools", module = "junit-foundation")
        }
    }
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(kotlinTest("junit5"))

    testImplementation(project(":kotlin-scripting-dependencies-maven"))

    testImplementation(intellijCore())
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    kotlinxSerializationGradlePluginClasspath(project(":kotlinx-serialization-compiler-plugin.embeddable")) { isTransitive = true }
    kotlinDataFrameGradlePluginClasspath(project(":kotlin-dataframe-compiler-plugin.embeddable")) { isTransitive = true }
    kotlinxCoroutinesCoreGradlePluginClasspath(libs.kotlinx.coroutines.core) { isTransitive = false }
    kotlinAllOpenPluginJar(project(":kotlin-allopen-compiler-plugin")) { isTransitive = false }
    kotlinMainKtsPluginJar(project(":kotlin-main-kts")) { isTransitive = false }
    kotlinScriptingCommonJar(project(":kotlin-scripting-common")) { isTransitive = false }
    powerAssertCompilerPluginJar(project(":kotlin-power-assert-compiler-plugin")) { isTransitive = false }
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

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        val scriptClasspath = testSourceSet.output.classesDirs
        addClasspathProperty(scriptClasspath,"kotlin.test.script.classpath")
        addClasspathProperty(kotlinxSerializationGradlePluginClasspath, "kotlin.script.test.kotlinx.serialization.plugin.classpath")
        addClasspathProperty(kotlinDataFrameGradlePluginClasspath, "kotlin.script.test.kotlin.dataframe.plugin.classpath")
        addClasspathProperty(kotlinxCoroutinesCoreGradlePluginClasspath, "kotlin.script.test.kotlinx.coroutines.core.classpath")
        addClasspathProperty(kotlinAllOpenPluginJar, "kotlin.allopen.plugin.jar")
        addClasspathProperty(kotlinMainKtsPluginJar, "kotlin.main.kts.plugin.jar")
        addClasspathProperty(kotlinScriptingCommonJar, "kotlin.scripting.common.jar")
        addClasspathProperty(powerAssertCompilerPluginJar, "kotlin.power.assert.compiler.plugin.jar")
    }

    testData(isolated, "testData")

    withJvmStdlibAndReflect()
    withStdlibCommon()
    withThirdPartyAnnotations()
    withThirdPartyJsr305()
    withThirdPartyJava8Annotations()
    withStdlibCommon()
    withJsRuntime()
    withWasmRuntime()
    withScriptRuntime()
    withTestJar()
    withMockJdkRuntime()
    @OptIn(KotlinCompilerDistUsage::class)
    withDist()
}
