package org.jetbrains.kotlin.abicmp.checkers

import org.jetbrains.kotlin.abicmp.defects.DefectType
import org.jetbrains.kotlin.abicmp.defects.METHOD_A
import org.jetbrains.kotlin.abicmp.isPrivate
import org.jetbrains.kotlin.abicmp.isSynthetic
import org.jetbrains.kotlin.abicmp.listOfNotNull
import org.jetbrains.kotlin.abicmp.methodFlags
import org.jetbrains.kotlin.abicmp.reports.ClassReport
import org.jetbrains.kotlin.abicmp.reports.ListEntryDiff
import org.jetbrains.kotlin.abicmp.tasks.methodId
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class MethodsListChecker : ClassChecker {
    override val name = "class.methods"

    val missing1Defect = DefectType("${name}.missing1", "Missing method in #1", METHOD_A)
    val missing2Defect = DefectType("${name}.missing2", "Missing method in #2", METHOD_A)

    private val ignoreMissingMethod1IfMethod2LooksLikeClosureConverted = true

    override fun check(class1: ClassNode, class2: ClassNode, report: ClassReport) {
        val methods1 = class1.loadMethods()
        val methods2 = class2.loadMethods()

        val relevantMethodIds = methods1.keys.union(methods2.keys)
            .filter {
                val method1 = methods1[it]
                val method2 = methods2[it]
                acceptNonSyntheticMethods(method1, method2) &&
                        !ignoreMissingClosureConvertedMethod1(method1, method2)
            }.toSet()

        val methodIds1 = methods1.keys.intersect(relevantMethodIds).sorted()
        val methodIds2 = methods2.keys.intersect(relevantMethodIds).sorted()

        val listDiff = compareLists(methodIds1, methodIds2) ?: return
        report.addMethodListDiffs(
            this,
            listDiff.map {
                ListEntryDiff(
                    it.value1?.toMethodWithFlags(methods1),
                    it.value2?.toMethodWithFlags(methods2)
                )
            }
        )
    }

    private fun acceptNonSyntheticMethods(method1: MethodNode?, method2: MethodNode?) =
        method1 != null && !method1.access.isSynthetic() ||
                method2 != null && !method2.access.isSynthetic()

    private fun ignoreMissingClosureConvertedMethod1(method1: MethodNode?, method2: MethodNode?) =
        ignoreMissingMethod1IfMethod2LooksLikeClosureConverted &&
                method1 == null && method2 != null &&
                method2.access.isPrivate() &&
                method2.name.contains('$')

    private fun String.toMethodWithFlags(methods: Map<String, MethodNode>): String {
        val method = methods[this] ?: return this
        return "$this ${method.access.methodFlags()}"
    }
}

fun ClassNode.loadMethods(): Map<String, MethodNode> =
    methods.listOfNotNull<MethodNode>().filter {
        (it.access and Opcodes.ACC_PUBLIC) != 0 ||
                (it.access and Opcodes.ACC_PROTECTED) != 0
    }.associateBy { it.methodId() }
