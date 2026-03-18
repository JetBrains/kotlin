/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import com.github.gradle.node.NodeExtension
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.build.d8.D8Extension

/**
 * @param taskName Name of Gradle task.
 * @param tag Optional JUnit test tag. See https://junit.org/junit5/docs/current/user-guide/#writing-tests-tagging-and-filtering
 */
@Suppress("UNCHECKED_CAST")
fun ProjectTestsExtension.jsTestTask(
    taskName: String = "test",
    tag: String? = null,
    skipInLocalBuild: Boolean = false,
    body: Test.() -> Unit = {},
): TaskProvider<Test> = testTask(
    taskName = taskName,
    jUnitMode = JUnitMode.JUnit5,
    skipInLocalBuild = skipInLocalBuild,
) {
    extensions.configure<TestInputsCheckExtension> {
        allowFlightRecorder.set(true)
    }

    val project = this@jsTestTask.project

    with(project.the<D8Extension>()) {
        setupV8()
    }

    jvmArgumentProviders += project.objects.newInstance<SystemPropertyClasspathProvider>().apply {
        classpath.from(project.rootDir.resolve("js/js.tests/testFixtures/org/jetbrains/kotlin/js/engine/repl.js"))
        property.set("javascript.engine.path.repl")
    }

    val node = project.the<NodeExtension>()
    systemProperty("kotlin.js.test.root.out.dir", "${node.nodeProjectDir.get().asFile}/")

    useJUnitPlatform {
        tag?.let { includeTags(it) }
    }

    body()
} as TaskProvider<Test>
