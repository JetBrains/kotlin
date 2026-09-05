/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.test.api

import org.jetbrains.dokka.analysis.test.api.jvm.java.JavaTestProject
import org.jetbrains.dokka.analysis.test.api.jvm.kotlin.KotlinJvmTestProject
import org.jetbrains.dokka.analysis.test.api.jvm.mixed.MixedJvmTestProject
import org.jetbrains.dokka.analysis.test.api.util.AnalysisTestDslMarker

/**
 * Creates a single-target Kotlin/JVM test project that only has Kotlin source code.
 *
 * See [javaTestProject] and [mixedJvmTestProject] if you want to check interoperability
 * with other JVM languages.
 *
 * By default, the sources are put in `/src/main/kotlin`, and the JVM version of Kotlin's
 * standard library is available on classpath.
 *
 * See [parse] and [useServices] functions to learn how to run Dokka with this project as input.
 *
 * @sample org.jetbrains.dokka.analysis.test.jvm.kotlin.SampleKotlinJvmAnalysisTest.sample
 */
fun kotlinJvmTestProject(init: (@AnalysisTestDslMarker KotlinJvmTestProject).() -> Unit): TestProject {
    val testData = KotlinJvmTestProject()
    testData.init()
    return testData
}

/**
 * Creates a Java-only test project.
 *
 * This can be used to test Dokka's Java support or specific
 * corner cases related to parsing Java sources.
 *
 * By default, the sources are put in `/src/main/java`. No Kotlin source code is allowed.
 *
 * See [parse] and [useServices] functions to learn how to run Dokka with this project as input.
 *
 * @sample org.jetbrains.dokka.analysis.test.jvm.java.SampleJavaAnalysisTest.sample
 */
fun javaTestProject(init: (@AnalysisTestDslMarker JavaTestProject).() -> Unit): TestProject {
    val testData = JavaTestProject()
    testData.init()
    return testData
}

/**
 * Creates a project where a number of JVM language sources are allowed,
 * like Java and Kotlin sources co-existing in the same source directory.
 *
 * This can be used to test interoperability between JVM languages.
 *
 * By default, this project consists of a single "jvm" source set, which has two source root directories:
 * * `/src/main/kotlin`
 * * `/src/main/java`
 *
 * See [parse] and [useServices] functions to learn how to run Dokka with this project as input.
 *
 * @sample org.jetbrains.dokka.analysis.test.jvm.mixed.SampleMixedJvmAnalysisTest.sample
 */
fun mixedJvmTestProject(init: (@AnalysisTestDslMarker MixedJvmTestProject).() -> Unit): TestProject {
    val testProject = MixedJvmTestProject()
    testProject.init()
    return testProject
}
