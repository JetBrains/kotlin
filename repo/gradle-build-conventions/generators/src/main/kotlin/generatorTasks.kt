/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.ide.idea.model.IdeaModel

fun Project.generatedDiagnosticContainersAndCheckerComponents(): TaskProvider<JavaExec> {
    val generatorClasspath: Configuration by configurations.creating

    dependencies {
        generatorClasspath(project(":compiler:fir:checkers:checkers-component-generator"))
    }

    val generationRoot = layout.projectDirectory.dir("gen")
    val task = tasks.register<JavaExec>("generateCheckersComponents") {
        workingDir = rootDir
        classpath = generatorClasspath
        mainClass.set("org.jetbrains.kotlin.fir.checkers.generator.MainKt")
        systemProperties["line.separator"] = "\n"
        args(project.name, generationRoot)
        outputs.dir(generationRoot)
    }

    sourceSets.named("main") {
        java.srcDirs(task)
    }

    if (kotlinBuildProperties.isInIdeaSync) {
        apply(plugin = "idea")
        (this as org.gradle.api.plugins.ExtensionAware).extensions.configure<IdeaModel>("idea") {
            this.module.generatedSourceDirs.add(generationRoot.asFile)
        }
    }
    return task
}

private val Project.sourceSets: SourceSetContainer
    get() = extensions.getByType<JavaPluginExtension>().sourceSets
