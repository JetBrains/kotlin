/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle

import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter

typealias IRsListBuilderFunction = GradleIRListBuilder.() -> Unit

fun IRsListBuilderFunction.build() = GradleIRListBuilder.build(this)

interface GradleIRBuilderBase {
    fun const(const: String) = GradleStringConstIR(const)
    fun raw(renderer: GradlePrinter.() -> Unit) = RawGradleIR(renderer)
    fun raw(string: String) = RawGradleIR { +string }

    fun new(@NonNls name: String, vararg parameters: BuildSystemIR) = GradleCallIr(name, parameters.toList(), isConstructorCall = true)
    fun new(@NonNls name: String, parameters: IRsListBuilderFunction) = new(name, *parameters.build().toTypedArray())

    fun fromString(renderer: GradlePrinter.() -> String) = RawGradleIR { +renderer() }
}

class GradleIRListBuilder private constructor() : GradleIRBuilderBase {
    private val irs = mutableListOf<BuildSystemIR>()

    operator fun String.invoke(inner: IRsListBuilderFunction) =
        GradleSectionIR(this, build(inner)).also(irs::add)

    operator fun String.invoke(vararg parameters: BuildSystemIR) =
        GradleCallIr(this, parameters.toList()).also(irs::add)


    infix fun String.assign(assignee: BuildSystemIR) =
        GradleAssignmentIR(this, assignee).also(irs::add)

    infix fun String.createValue(assignee: BuildSystemIR) =
        CreateGradleValueIR(this, assignee).also(irs::add)

    operator fun String.unaryPlus() = RawGradleIR { +this@unaryPlus }.also(irs::add)
    operator fun BuildSystemIR.unaryPlus() = also(irs::add)
    operator fun Collection<BuildSystemIR>.unaryPlus() = also(irs::addAll)

    fun addRaw(raw: GradlePrinter.() -> Unit) = RawGradleIR(raw).also(irs::add)
    fun addRaw(@NonNls raw: String) = RawGradleIR { +raw }.also(irs::add)

    fun import(@NonNls import: String) = GradleImportIR(import).also(irs::add)

    fun build(): List<BuildSystemIR> = irs

    companion object {
        fun build(inner: IRsListBuilderFunction) = GradleIRListBuilder().apply(inner).build()
    }
}

fun irsList(inner: IRsListBuilderFunction): List<BuildSystemIR> =
    GradleIRListBuilder.build(inner)