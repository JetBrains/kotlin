package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.BuildSystemIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.IrsOwner
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.KotlinIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.GradleCallIr
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.GradleIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.IRsListBuilderFunction
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.build
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.withIrs
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleSubType
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter

interface MultiplatformIR : GradleIR, KotlinIR

interface TargetIR : MultiplatformIR

data class TargetAccessIR(
    val type: ModuleSubType,
    val nonDefaultName: String?, // `null` stands for default target name
    val additionalParams: List<Any> = listOf()
) : TargetIR {
    override fun GradlePrinter.renderGradle() {
        +type.toString()
        par {
            nonDefaultName?.let {
                +it.quotified
                if (additionalParams.isNotEmpty()) +", "
            }
            if (additionalParams.isNotEmpty()) {
                additionalParams
                    .joinToString {
                        it.toString()
                    }
                    .apply { +this }
            }
        }
    }
}

interface TargetConfigurationIR : MultiplatformIR, IrsOwner {
    val targetName: String
}

fun TargetConfigurationIR.addWithJavaIntoJvmTarget() = when {
    this is DefaultTargetConfigurationIR
            && targetAccess.type == ModuleSubType.jvm
            && irs.none { it is GradleCallIr } ->
        withIrs(GradleCallIr("withJava"))
    else -> this
}

data class DefaultTargetConfigurationIR(
    val targetAccess: TargetAccessIR,
    override val irs: PersistentList<BuildSystemIR>
) : TargetConfigurationIR {
    override val targetName: String
        get() = targetAccess.nonDefaultName ?: targetAccess.type.name

    constructor(targetAccess: TargetAccessIR, irs: IRsListBuilderFunction)
            : this(targetAccess, irs.build().toPersistentList())

    override fun withReplacedIrs(irs: PersistentList<BuildSystemIR>): DefaultTargetConfigurationIR =
        copy(irs = irs)

    override fun GradlePrinter.renderGradle() {
        +targetAccess.type.toString()
        if (irs.isEmpty() || targetAccess.nonDefaultName != null || targetAccess.additionalParams.isNotEmpty()) par {
            targetAccess.nonDefaultName?.let {
                +it.quotified
                if (targetAccess.additionalParams.isNotEmpty()) +", "
            }
            if (targetAccess.additionalParams.isNotEmpty()) {
                targetAccess.additionalParams
                    .joinToString {
                        it.toString()
                    }
                    .apply { +this }
            }
        }
        if (irs.isNotEmpty()) {
            +" "; inBrackets { irs.listNl() }
        }
    }
}

data class NonDefaultTargetConfigurationIR(
    val variableName: String,
    override val targetName: String,
    override val irs: PersistentList<BuildSystemIR>
) : TargetConfigurationIR {
    override fun withReplacedIrs(irs: PersistentList<BuildSystemIR>): NonDefaultTargetConfigurationIR =
        copy(irs = irs)

    constructor(variableName: String, targetName: String, irs: IRsListBuilderFunction)
            : this(variableName, targetName, irs.build().toPersistentList())

    override fun GradlePrinter.renderGradle() {
        if (irs.isNotEmpty()) {
            +variableName
            when (dsl) {
                GradlePrinter.GradleDsl.KOTLIN -> +".apply"
                GradlePrinter.GradleDsl.GROOVY -> +".with"
            }
            +" "; inBrackets { irs.listNl() }
        }
    }
}

data class CompilationIR(
    val name: String,
    override val irs: PersistentList<BuildSystemIR>
) : MultiplatformIR, IrsOwner {
    override fun withReplacedIrs(irs: PersistentList<BuildSystemIR>) = copy(irs = irs)
    override fun GradlePrinter.renderGradle() {
        getting(name, "compilations") { irs.listNl() }
    }
}
