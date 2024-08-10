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
            languageVersion.set(JavaLanguageVersion.of(17))
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
    compileOnly(intellijJDom())
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
    testRuntimeOnly(intellijJDom())
    testRuntimeOnly(libs.kotlinx.coroutines.core.jvm)

    testImplementation(projectTests(":compiler:incremental-compilation-impl"))
    testImplementation(jpsBuild())

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
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.compileJava {
    sourceCompatibility = "11"
    targetCompatibility = "11"
}

tasks.compileKotlin {
    compilerOptions.jvmTarget = JvmTarget.JVM_11
}

projectTest(parallel = true) {
    // do not replace with compile/runtime dependency,
    // because it forces Intellij reindexing after each compiler change
    dependsOn(":kotlin-compiler:dist")
    dependsOn(":kotlin-stdlib:jsJarForTests")
    workingDir = rootDir
    jvmArgs(
        // https://github.com/JetBrains/intellij-community/blob/b49faf433f8d73ccd46016a5717f997d167de65f/jps/jps-builders/src/org/jetbrains/jps/cmdline/ClasspathBootstrap.java#L67
        "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED",
        // the minimal required set of modules to be opened for the intellij platform itself
        "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
        "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.desktop/javax.swing=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
    )
}

testsJar {}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions.apiVersion.value(KotlinVersion.KOTLIN_1_8).finalizeValueOnRead()
    compilerOptions.languageVersion.value(KotlinVersion.KOTLIN_1_8).finalizeValueOnRead()
}