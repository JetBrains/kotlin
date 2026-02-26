@file:Suppress("unused") // usages in build scripts are not tracked properly

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.register

const val kotlinEmbeddableRootPackage = "org.jetbrains.kotlin"
const val EMBEDDABLE_COMPILER_TASK_NAME = "embeddable"

val packagesToRelocate =
    listOf(
        "com.google",
        "com.sampullara",
        "org.apache",
        "org.jdom",
        "org.picocontainer",
        "org.jline",
        "org.fusesource",
        "net.jpountz",
        "one.util.streamex",
        "it.unimi.dsi.fastutil",
        "kotlinx.collections.immutable",
        "com.fasterxml",
        "org.codehaus",
        "io.opentelemetry",
        "io.vavr",
        "org.antlr"
    )

fun ShadowJar.configureEmbeddableCompilerRelocation(withJavaxInject: Boolean = true) {
    relocate("com.google.protobuf", "org.jetbrains.kotlin.protobuf")
    relocate("com.intellij", "$kotlinEmbeddableRootPackage.com.intellij") {
        // These are not real packages, but important string constants which are used by xml-reader.
        exclude("com.intellij.projectService")
        exclude("com.intellij.applicationService")
    }
    packagesToRelocate.forEach {
        relocate(it, "$kotlinEmbeddableRootPackage.$it")
    }
    if (withJavaxInject) {
        relocate("javax.inject", "$kotlinEmbeddableRootPackage.javax.inject")
    }
    relocate("org.fusesource", "$kotlinEmbeddableRootPackage.org.fusesource") {
        exclude("org.fusesource.jansi.internal.CLibrary")
    }

    /**
     * Workaround for
     * https://github.com/GradleUp/shadow/issues/1929
     */
    enableKotlinModuleRemapping.set(false)
}

private fun Project.compilerShadowJar(taskName: String, body: ShadowJar.() -> Unit): TaskProvider<ShadowJar> {

    val compilerJar = configurations.dependencyScope("compilerJar")
    val compilerJarClasspath = configurations.resolvable("compilerJarClasspath") {
        extendsFrom(compilerJar.get())
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        }
    }

    dependencies.add(compilerJar.name, dependencies.project(":kotlin-compiler")) { isTransitive = false }

    return tasks.register<ShadowJar>(taskName) {
        destinationDirectory.set(project.layout.buildDirectory.dir("libs"))
        configurations.set(listOf(compilerJarClasspath.get()))
        body()
    }
}

fun Project.embeddableCompiler(
    taskName: String = EMBEDDABLE_COMPILER_TASK_NAME,
    body: ShadowJar.() -> Unit = {}
): TaskProvider<ShadowJar> =
    compilerShadowJar(taskName) {
        configureEmbeddableCompilerRelocation()
        body()
    }

fun Project.rewriteDefaultJarDepsToShadedCompiler(
    body: ShadowJar.() -> Unit = {}
): TaskProvider<ShadowJar> {
    val jarTask = tasks.named<Jar>("jar")
    jarTask.configure {
        archiveClassifier.set("original")
    }

    return tasks.register<ShadowJar>(EMBEDDABLE_COMPILER_TASK_NAME) {
        from(jarTask)

        archiveClassifier.unset()

        destinationDirectory.set(project.layout.buildDirectory.dir("libs"))
        configureEmbeddableCompilerRelocation(withJavaxInject = false)
        body()
    }
}
