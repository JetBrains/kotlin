/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.build.d8.D8Extension
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.attributes.KlibPackaging

@OptIn(ExperimentalKotlinGradlePluginApi::class)
fun Test.useJsIrBoxTests(
    buildDir: Provider<Directory>,
) {
    with(project.the<D8Extension>()) {
        setupV8()
    }

    val stdLibJsClasses = project.configurations.maybeCreate("stdLibJsClasses").apply {
        isTransitive = false
        attributes {
            attribute(KlibPackaging.ATTRIBUTE, project.objects.named(KlibPackaging.NON_PACKED))
        }
    }
    val jsDomApiCompatClasses = project.configurations.maybeCreate("jsDomApiCompatClasses").apply {
        isTransitive = false
        attributes {
            attribute(KlibPackaging.ATTRIBUTE, project.objects.named(KlibPackaging.NON_PACKED))
        }
    }
    val stdlibJsIrMinimalForTestClasses = project.configurations.maybeCreate("stdlibJsIrMinimalForTestClasses").apply {
        isTransitive = false
        attributes {
            attribute(KlibPackaging.ATTRIBUTE, project.objects.named(KlibPackaging.NON_PACKED))
        }
    }
    project.dependencies {
        add(stdLibJsClasses.name, project(":kotlin-stdlib", "jsRuntimeElements"))
        add(jsDomApiCompatClasses.name, project(":kotlin-dom-api-compat", "jsRuntimeElements"))
        add(stdlibJsIrMinimalForTestClasses.name, project(":kotlin-stdlib-js-ir-minimal-for-test", "jsRuntimeElements"))
    }
    addClasspathProperty(stdLibJsClasses, "kotlin.js.full.stdlib.path")
    addClasspathProperty(jsDomApiCompatClasses, "kotlin.js.dom.api.compat")
    addClasspathProperty(stdlibJsIrMinimalForTestClasses, "kotlin.js.reduced.stdlib.path")

    systemProperty("kotlin.js.test.root.out.dir", "${buildDir.get().asFile.relativeTo(project.projectDir)}/")

    jvmArgumentProviders += project.objects.newInstance<SystemPropertyClasspathProvider>().apply {
        classpath.from(project.rootDir.resolve("js/js.tests/testFixtures/org/jetbrains/kotlin/js/engine/repl.js"))
        property.set("javascript.engine.path.repl")
    }
}
