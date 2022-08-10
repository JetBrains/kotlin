/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.model.infra

import org.jetbrains.kotlin.project.model.KpmLocalModuleIdentifier
import org.jetbrains.kotlin.project.model.KpmMavenModuleIdentifier
import org.jetbrains.kotlin.project.model.nativeTarget
import org.jetbrains.kotlin.project.model.platform
import org.jetbrains.kotlin.utils.Printer

@Suppress("unused") // useful for debugging
fun KpmTestCase.renderDeclarationDsl(): String {
    val p = Printer(StringBuilder())
    p.render(this)
    return p.toString()
}

private fun Printer.render(case: KpmTestCase) {
    println("val ${case.name} = describeCase(\"${case.name}\") {")
    pushIndent()
    val projectsSorted = case.projects.sortedBy { it.name }
    for (project in projectsSorted) {
        render(project)
        if (project !== projectsSorted.last()) println()
    }
    popIndent()
    println("}")

}

private fun Printer.render(project: TestKpmModuleContainer) {
    println("project(\"${project.name}\") {")
    pushIndent()
    val modulesSorted = project.modules.sortedBy { it.name }
    for (module in modulesSorted) {
        render(module)
        if (module !== modulesSorted.last()) println()
    }
    popIndent()
    println("}")
}

private fun Printer.render(module: TestKpmModule) {
    println("module(\"${module.name}\") {")
    pushIndent()
    val fragmentsSorted = module.fragments.sortedBy { it.name }

    // printedFragmentDeclaration is needed for pretty-printing separating new line between fragments
    // declarations and their dependencies only in case both are present (i.e. no trailing newlines)
    var printedFragmentDeclaration = false
    for (fragment in fragmentsSorted) {
        printedFragmentDeclaration = printedFragmentDeclaration or renderFragmentDeclaration(fragment)
    }

    for (fragment in fragmentsSorted) {
        renderFragmentDependencies(fragment, printedFragmentDeclaration)
    }

    popIndent()
    println("}")
}

private fun Printer.renderFragmentDeclaration(fragment: TestKpmFragment): Boolean {
    if (fragment.name == "common") return false

    when {
        fragment !is TestKpmVariant -> println("fragment(\"${fragment.name}\")")
        fragment.platform == "jvm" -> println("jvm()")
        fragment.platform == "js" -> println("js()")
        fragment.nativeTarget == "linux" -> println("linux()")
        fragment.nativeTarget == "macosX64" -> println("macosX64()")
        fragment.nativeTarget == "android" -> println("android()")
        else -> error("Unknown platform: ${fragment.platform}, nativeTarget=${fragment.nativeTarget}")
    }
    return true
}

private fun Printer.renderFragmentDependencies(fragment: TestKpmFragment, prependWithEmptyLine: Boolean) {
    var printEmptyLineOnce: Boolean = prependWithEmptyLine
    fun println(text: String) {
        if (printEmptyLineOnce) {
            this.println()
            printEmptyLineOnce = false
        }
        this.println(text)
    }

    for (refinedFragment in fragment.declaredRefinesDependencies.sortedBy { it.name }) {
        if (refinedFragment.name == "common") continue
        println("${fragment.name} refines ${refinedFragment.name}")
    }

    val moduleDependenciesSorted = fragment.declaredModuleDependencies.sortedBy { it.moduleIdentifier.toString() }
    for (dependencyModule in moduleDependenciesSorted) {
        when (val id = dependencyModule.moduleIdentifier) {
            is KpmLocalModuleIdentifier -> {
                val projectId = id.projectId
                println("${fragment.name} depends ${"project(\"$projectId\")"}")
            }

            is KpmMavenModuleIdentifier -> {
                val group = id.group
                val name = id.name
                println("${fragment.name} depends ${"maven(\"$group\", \"$name\")"}")
            }
        }
    }
}
