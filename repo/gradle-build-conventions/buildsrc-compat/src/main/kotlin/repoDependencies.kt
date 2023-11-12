/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("RepoDependencies")

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.internal.jvm.Jvm
import org.gradle.kotlin.dsl.closureOf
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.project
import java.io.File

private val Project.isEAPIntellij get() = rootProject.extra["versions.intellijSdk"].toString().contains("-EAP-")
private val Project.isNightlyIntellij get() = rootProject.extra["versions.intellijSdk"].toString().endsWith("SNAPSHOT") && !isEAPIntellij

val Project.intellijRepo
    get() =
        when {
            isEAPIntellij -> "https://www.jetbrains.com/intellij-repository/snapshots"
            isNightlyIntellij -> "https://www.jetbrains.com/intellij-repository/nightly"
            else -> "https://www.jetbrains.com/intellij-repository/releases"
        }

fun Project.commonDependency(coordinates: String): String {
    val parts = coordinates.split(':')
    return when (parts.size) {
        1 -> "$coordinates:$coordinates:${commonDependencyVersion(coordinates, coordinates)}"
        2 -> "${parts[0]}:${parts[1]}:${commonDependencyVersion(parts[0], parts[1])}"
        3 -> coordinates
        else -> throw IllegalArgumentException("Illegal maven coordinates: $coordinates")
    }
}

fun Project.commonDependency(group: String, artifact: String, vararg suffixesAndClassifiers: String): String {
    val (classifiers, artifactSuffixes) = suffixesAndClassifiers.partition { it.startsWith(':') }
    return "$group:$artifact${artifactSuffixes.joinToString("")}:${commonDependencyVersion(group, artifact)}${classifiers.joinToString("")}"
}

fun Project.commonDependencyVersion(group: String, artifact: String): String =
    when {
        rootProject.extra.has("versions.$artifact") -> rootProject.extra["versions.$artifact"]
        rootProject.extra.has("versions.$group") -> rootProject.extra["versions.$group"]
        else -> throw GradleException("Neither versions.$artifact nor versions.$group is defined in the root project's extra")
    } as String

fun kotlinDep(artifactBaseName: String, version: String, classifier: String? = null): String =
    listOfNotNull("org.jetbrains.kotlin:kotlin-$artifactBaseName:$version", classifier).joinToString(":")

@JvmOverloads
fun Project.kotlinStdlib(suffix: String? = null, classifier: String? = null): Any {
    return if (kotlinBuildProperties.useBootstrapStdlib)
        kotlinDep(listOfNotNull("stdlib", suffix).joinToString("-"), bootstrapKotlinVersion, classifier)
    else
        dependencies.project(listOfNotNull(":kotlin-stdlib", suffix).joinToString("-"), classifier)
}

/**
 * Use this function to declare a dependency on kotlin-test project artifacts.
 *
 * It creates either a project dependency or a binary dependency on bootstrap artifacts when JPS build is imported.
 *
 * @param suffix Supported suffixes are:
 * - `null` for the default project dependency, variant is resolved by attributes
 * - `junit`, `junit5`, `testng` - jvm variants with annotation typealiases for different test frameworks,
 * - `js` - js variant with assertions and annotations
 * @param classifier Supported classifiers are: `null` for the runtime artifact, `sources` for the sources jar artifact
 */
@JvmOverloads
fun Project.kotlinTest(suffix: String? = null, classifier: String? = null): Any {
    return if (kotlinBuildProperties.isInJpsBuildIdeaSync) {
        kotlinDep(listOfNotNull("test", suffix?.lowercase()).joinToString("-"), bootstrapKotlinVersion, classifier)
    } else {
        val elementsType = when (classifier) {
            null -> "Runtime"
            "sources" -> "Sources"
            else -> error("Unsupported kotlin-test classifier: $classifier")
        }
        val configuration = when (suffix?.lowercase()) {
            null -> classifier?.let { "jvm${elementsType}Elements" }
            "junit" -> "jvmJUnit${elementsType}Elements"
            "junit5" -> "jvmJUnit5${elementsType}Elements"
            "testng" -> "jvmTestNG${elementsType}Elements"
            "js" -> "js${elementsType}Elements"
            else -> error("Unsupported kotlin-test flavor: $suffix")
        }
        dependencies.project(":kotlin-test", configuration)
    }
}

fun Project.kotlinBuiltins(): Any = kotlinBuiltins(forJvm = false)

fun Project.kotlinBuiltins(forJvm: Boolean): Any =
    if (kotlinBuildProperties.useBootstrapStdlib) "org.jetbrains.kotlin:builtins:$bootstrapKotlinVersion"
    else dependencies.project(":core:builtins", configuration = "runtimeElementsJvm".takeIf { forJvm })

fun DependencyHandler.projectTests(name: String): ProjectDependency = project(name, configuration = "tests-jar")

enum class JpsDepScope {
    COMPILE, TEST, RUNTIME, PROVIDED
}

fun DependencyHandler.add(configurationName: String, dependencyNotation: Any, configure: (ModuleDependency.() -> Unit)?) {
    // Avoid `dependencyNotation` to `ModuleDependency` class cast exception if possible
    if (configure != null) {
        add(configurationName, dependencyNotation, closureOf(configure))
    } else {
        add(configurationName, dependencyNotation)
    }
}

@Suppress("unused") // Used in cooperative mode with IDEA Kotlin plugin
fun Project.disableDependencyVerification() {
    configurations.all {
        resolutionStrategy {
            disableDependencyVerification()
        }
    }
}

fun DependencyHandler.jpsLikeJarDependency(
    dependencyNotation: Any,
    scope: JpsDepScope,
    dependencyConfiguration: (ModuleDependency.() -> Unit)? = null,
    exported: Boolean = false
) {
    when (scope) {
        JpsDepScope.COMPILE -> {
            if (exported) {
                add("api", dependencyNotation, dependencyConfiguration)
                add("testApi", dependencyNotation, dependencyConfiguration)
            } else {
                add("implementation", dependencyNotation, dependencyConfiguration)
            }
        }
        JpsDepScope.TEST -> {
            if (exported) {
                add("testApi", dependencyNotation, dependencyConfiguration)
            } else {
                add("testImplementation", dependencyNotation, dependencyConfiguration)
            }
        }
        JpsDepScope.RUNTIME -> {
            add("testRuntimeOnly", dependencyNotation, dependencyConfiguration)
        }
        JpsDepScope.PROVIDED -> {
            if (exported) {
                add("compileOnlyApi", dependencyNotation, dependencyConfiguration)
                add("testApi", dependencyNotation, dependencyConfiguration)
            } else {
                add("compileOnly", dependencyNotation, dependencyConfiguration)
                add("testImplementation", dependencyNotation, dependencyConfiguration)
            }
        }
    }
}

@Suppress("unused") // Used in cooperative mode with IDEA Kotlin plugin
fun DependencyHandler.jpsLikeModuleDependency(moduleName: String, scope: JpsDepScope, exported: Boolean = false) {
    jpsLikeJarDependency(project(moduleName), scope, exported = exported)
    when (scope) {
        JpsDepScope.COMPILE -> {
            if (exported) {
                add("testApi", projectTests(moduleName))
            } else {
                add("testImplementation", projectTests(moduleName))
            }
        }
        JpsDepScope.TEST -> {
            if (exported) {
                add("testApi", projectTests(moduleName))
            } else {
                add("testImplementation", projectTests(moduleName))
            }
        }
        JpsDepScope.RUNTIME -> {
            add("runtimeOnly", projectTests(moduleName))
        }
        JpsDepScope.PROVIDED -> {
            if (exported) {
                add("testApi", projectTests(moduleName))
            } else {
                add("testImplementation", projectTests(moduleName))
            }
        }
    }
}

val Project.protobufRelocatedVersion: String get() = findProperty("versions.protobuf-relocated") as String
fun Project.protobufLite(): String = "org.jetbrains.kotlin:protobuf-lite:$protobufRelocatedVersion"
fun Project.protobufFull(): String = "org.jetbrains.kotlin:protobuf-relocated:$protobufRelocatedVersion"
fun Project.kotlinxCollectionsImmutable() =
    "org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:${rootProject.extra["versions.kotlinx-collections-immutable"]}"

val Project.kotlinNativeVersion: String get() = property("versions.kotlin-native") as String

val Project.nodejsVersion: String get() = property("versions.nodejs") as String
val Project.v8Version: String get() = property("versions.v8") as String

fun File.matchMaybeVersionedArtifact(baseName: String) = name.matches(baseName.toMaybeVersionedJarRegex())

private val wildcardsRe = """[^*?]+|(\*)|(\?)""".toRegex()

private fun String.wildcardsToEscapedRegexString(): String = buildString {
    wildcardsRe.findAll(this@wildcardsToEscapedRegexString).forEach {
        when {
            it.groups[1] != null -> append(".*")
            it.groups[2] != null -> append(".")
            else -> append("\\Q${it.groups[0]!!.value}\\E")
        }
    }
}

private fun String.toMaybeVersionedJarRegex(): Regex {
    val hasJarExtension = endsWith(".jar")
    val escaped = this.wildcardsToEscapedRegexString()
    return Regex(if (hasJarExtension) escaped else "$escaped(-\\d.*)?\\.jar") // TODO: consider more precise version part of the regex
}

fun Project.firstFromJavaHomeThatExists(
    vararg paths: String,
    jdkHome: File = File((this.property("JDK_1_8") ?: this.property("JDK_18") ?: error("Can't find JDK_1_8 property")) as String)
): File? =
    paths.map { File(jdkHome, it) }.firstOrNull { it.exists() }.also {
        if (it == null)
            logger.warn("Cannot find file by paths: ${paths.toList()} in $jdkHome")
    }

fun Project.toolsJarApi(): Any =
    if (kotlinBuildProperties.isInJpsBuildIdeaSync)
        toolsJar()
    else
        dependencies.project(":dependencies:tools-jar-api")

fun Project.toolsJar(): FileCollection = files(
    getToolchainLauncherFor(DEFAULT_JVM_TOOLCHAIN)
        .map {
            Jvm.forHome(it.metadata.installationPath.asFile).toolsJar ?: throw GradleException("tools.jar not found!")
        }
)

val compilerManifestClassPath
    get() = "annotations-13.0.jar kotlin-stdlib.jar kotlin-reflect.jar kotlin-script-runtime.jar trove4j.jar"

object EmbeddedComponents {
    const val CONFIGURATION_NAME = "embedded"
}
