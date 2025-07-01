/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.test.services.JUnit5Assertions.fail

/*************** Process-level system properties ***************/

internal enum class ProcessLevelProperty(shortName: String) {
    KOTLIN_NATIVE_HOME("nativeHome"),
    COMPILER_CLASSPATH("compilerClasspath"),
    TEAMCITY("teamcity"),
    CUSTOM_KOTLIN_NATIVE_HOME("customNativeHome"),
    MINIDUMP_ANALYZER("minidumpAnalyzer");

    private val propertyName = fullPropertyName(shortName)

    fun readValue(): String = System.getProperty(propertyName) ?: fail { "Unspecified $propertyName system property" }
}

/*************** Class-level system properties ***************/

@Repeatable
@Target(AnnotationTarget.CLASS)
annotation class EnforcedProperty(val property: ClassLevelProperty, val propertyValue: String)

@Target(AnnotationTarget.CLASS)
annotation class EnforcedHostTarget

@Target(AnnotationTarget.CLASS)
internal annotation class AcceptablePropertyValues(val property: ClassLevelProperty, val acceptableValues: Array<String>)

class EnforcedProperties(testClass: Class<*>) {
    private val enforcedAnnotations: Map<ClassLevelProperty, String> = buildMap {
        testClass.getAnnotationsByType(EnforcedProperty::class.java).forEach {
            this[it.property] = it.propertyValue
        }
        if (testClass.isAnnotationPresent(EnforcedHostTarget::class.java)) {
            this[ClassLevelProperty.TEST_TARGET] = HostManager.host.name
        }
    }

    operator fun get(propertyType: ClassLevelProperty): String? = enforcedAnnotations[propertyType]

    private val acceptableAnnotations: Map<ClassLevelProperty, Array<String>> = testClass.annotations
        .filterIsInstance<AcceptablePropertyValues>()
        .associate {
            it.property to it.acceptableValues
        }

    fun isAcceptableValue(propertyType: ClassLevelProperty, value: String?): Boolean =
        acceptableAnnotations[propertyType]?.contains(value) ?: true
}

enum class ClassLevelProperty(val shortName: String) {
    TEST_TARGET("target"),
    TEST_MODE("mode"),
    COMPILER_PLUGINS("compilerPlugins"),
    CUSTOM_KLIBS("customKlibs"),
    TEST_KIND("testKind"),
    COMPILE_ONLY("compileOnly"),
    OPTIMIZATION_MODE("optimizationMode"),
    USE_THREAD_STATE_CHECKER("useThreadStateChecker"),
    GC_TYPE("gcType"),
    GC_SCHEDULER("gcScheduler"),
    ALLOCATOR("alloc"),
    CACHE_MODE("cacheMode"),
    EXECUTION_TIMEOUT("executionTimeout"),
    SANITIZER("sanitizer"),
    COMPILER_OUTPUT_INTERCEPTOR("compilerOutputInterceptor"),
    PIPELINE_TYPE("pipelineType"),
    SHARED_TEST_EXECUTION("sharedTestExecution"),
    BINARY_LIBRARY_KIND("binaryLibraryKind"),
    C_INTERFACE_MODE("cInterfaceMode"),
    XCTEST_FRAMEWORK("xctest"),
    BINARY_OPTIONS("binaryOptions"),
    ;

    internal val propertyName = fullPropertyName(shortName)

    fun <T> readValue(enforcedProperties: EnforcedProperties, transform: (String) -> T?, default: T): T {
        val propertyValue = enforcedProperties[this] ?: System.getProperty(propertyName)
        val acceptable = enforcedProperties.isAcceptableValue(this, propertyValue)
        return if (propertyValue != null && acceptable) {
            transform(propertyValue) ?: fail { "Invalid value for $propertyName system property: $propertyValue" }
        } else
            default
    }
}

internal inline fun <reified E : Enum<E>> ClassLevelProperty.readValue(
    enforcedProperties: EnforcedProperties,
    values: Array<out E>,
    default: E
): E = readValueOrNull(enforcedProperties, values) ?: default

internal inline fun <reified E : Enum<E>> ClassLevelProperty.readValueOrNull(
    enforcedProperties: EnforcedProperties,
    values: Array<out E>,
): E? {
    val optionName = enforcedProperties[this] ?: System.getProperty(propertyName)
    val acceptable = enforcedProperties.isAcceptableValue(this, optionName)
    return if (optionName != null && acceptable) {
        values.firstOrNull { it.name == optionName } ?: fail {
            buildString {
                appendLine("Unknown ${E::class.java.simpleName} name $optionName.")
                appendLine("One of the following ${E::class.java.simpleName} should be passed through $propertyName system property:")
                values.forEach { value -> appendLine("- ${value.name}: $value") }
            }
        }
    } else null
}

private fun fullPropertyName(shortName: String) = "kotlin.internal.native.test.$shortName"

/*************** Environment variables ***************/

internal enum class EnvironmentVariable {
    PROJECT_BUILD_DIR,
    GRADLE_TASK_NAME;

    fun readValue(): String = System.getenv(name) ?: fail { "Unspecified $name environment variable" }
}
