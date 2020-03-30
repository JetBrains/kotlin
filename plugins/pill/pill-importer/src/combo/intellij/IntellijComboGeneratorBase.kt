/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill.combo.intellij

import java.io.File

abstract class IntellijComboGeneratorBase(protected val kotlinProjectDir: File) {
    protected val comboProjectDir: File = kotlinProjectDir.parentFile
    protected val ideaProjectDir = File(kotlinProjectDir.parentFile, "intellij")

    protected val kotlinDependenciesDir = File(System.getProperty("user.home"), ".gradle/kotlin-build-dependencies/repo/kotlin.build")
    protected val kotlinPseudoDependenciesDir = File(kotlinProjectDir, "buildSrc/prepare-deps/intellijRepo")

    protected val substitutions: Substitutions

    init {
        require(kotlinProjectDir.name == "kotlin") { "Kotlin project must be placed in " + File(kotlinProjectDir.parentFile, "kotlin") }
        require(ideaProjectDir.exists()) { "Intellij project not found in $ideaProjectDir" }

        substitutions = SubstitutionFileReader.read(kotlinProjectDir, ideaProjectDir)
    }

    protected fun getDependenciesLocalPath(file: File): String? {
        if (file.startsWith(kotlinDependenciesDir)) {
            return file.toRelativeString(kotlinDependenciesDir)
        } else if (file.startsWith(kotlinPseudoDependenciesDir)) {
            val groupDir = File(kotlinPseudoDependenciesDir, IntellijRepoGenerator.GROUP_ID.replace('.', '/'))
            check(file.startsWith(groupDir)) { "File is outside ${groupDir} group directory: $file" }
            return file.toRelativeString(groupDir)
        }

        return null
    }
}