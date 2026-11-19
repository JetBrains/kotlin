/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/*
 * The buildHealth ratchet: issue types promoted to severity("fail") below fail `buildHealth` for every module
 * except those grandfathered in BuildHealthBaselines, which are pinned back to "warn" until they are fixed.
 * See BuildHealthBaselines for how to generate the baseline lists and how to promote a new issue type.
 */

plugins {
    id("com.autonomousapps.dependency-analysis")
}

val isKotlinNativeEnabled = project.kotlinBuildProperties.isKotlinNativeEnabled.get()

dependencyAnalysis {
    issues {
        all {
            onUnusedAnnotationProcessors { severity("fail")}
            onModuleStructure { severity("fail")}
            onDuplicateClassWarnings { severity("fail") }
        }

        val baselineProjects = if (isKotlinNativeEnabled) {
            BuildHealthBaselines.duplicateClassesBaselineProjects + BuildHealthBaselines.duplicateClassesBaselineProjectsNativeEnabled
        } else {
            BuildHealthBaselines.duplicateClassesBaselineProjects
        }

        baselineProjects.forEach { path ->
            project(path) {
                onDuplicateClassWarnings { severity("warn") }
            }
        }
    }
}
