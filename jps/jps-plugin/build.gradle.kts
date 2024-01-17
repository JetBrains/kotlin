import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

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
    compileOnly(project(":jps:jps-platform-api-signatures"))
    testImplementation(projectTests(":generators:test-generator"))

    @Suppress("UNCHECKED_CAST")
    rootProject.extra["kotlinJpsPluginEmbeddedDependencies"]
        .let { it as List<String> }
        .forEach { implementation(project(it)) }

    @Suppress("UNCHECKED_CAST")
    rootProject.extra["kotlinJpsPluginMavenDependencies"]
        .let { it as List<String> }
        .forEach { implementation(project(it)) }

    @Suppress("UNCHECKED_CAST")
    rootProject.extra["kotlinJpsPluginMavenDependenciesNonTransitiveLibs"]
        .let { it as List<String> }
        .forEach { implementation(it) { isTransitive = false } }

    implementation(project(":jps:jps-common"))
    compileOnly(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))
    compileOnly(jpsModel())
    compileOnly(jpsBuild())
    compileOnly(jpsModelSerialization())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:jdom"))
    testRuntimeOnly(jpsModel())

    // testFramework includes too many unnecessary dependencies. Here we manually list all we need to successfully run JPS tests
    testImplementation(jpsModelSerialization()) { isTransitive = false }
    testImplementation(testFramework()) { isTransitive = false }
    testImplementation("com.jetbrains.intellij.platform:test-framework-core:$intellijVersion") { isTransitive = false }
    testImplementation("com.jetbrains.intellij.platform:test-framework-common:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:analysis-impl:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:boot:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:analysis:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:project-model:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:object-serializer:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:code-style:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:ide-impl:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:ide:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:ide-core:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:ide-core-impl:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:execution:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:util-ui:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:concurrency:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:editor:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:core-ui:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:lang:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:lang-impl:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.jetbrains.intellij.platform:util-ex:$intellijVersion") { isTransitive = false }
    testRuntimeOnly("com.google.code.gson:gson:2.8.9")

    testImplementation(projectTests(":compiler:incremental-compilation-impl"))
    testCompileOnly(jpsBuild())

    compilerModules.forEach {
        testRuntimeOnly(project(it))
    }

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
    compilerOptions.jvmTarget = JvmTarget.JVM_1_8
}

projectTest(parallel = true) {
    // do not replace with compile/runtime dependency,
    // because it forces Intellij reindexing after each compiler change
    dependsOn(":kotlin-compiler:dist")
    dependsOn(":kotlin-stdlib:jsJarForTests")
    workingDir = rootDir
}

testsJar {}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions.apiVersion.value(KotlinVersion.KOTLIN_1_8).finalizeValueOnRead()
    compilerOptions.languageVersion.value(KotlinVersion.KOTLIN_1_8).finalizeValueOnRead()
}