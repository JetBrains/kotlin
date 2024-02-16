/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abicmp.tasks

import kotlin.metadata.jvm.KotlinClassMetadata
import org.jetbrains.kotlin.abicmp.checkers.loadFields
import org.jetbrains.kotlin.abicmp.checkers.loadMethods
import org.jetbrains.kotlin.abicmp.classFlags
import org.jetbrains.kotlin.abicmp.isSynthetic
import org.jetbrains.kotlin.abicmp.reports.ClassReport
import org.jetbrains.kotlin.abicmp.reports.ListEntryDiff
import org.jetbrains.kotlin.abicmp.tag
import org.jetbrains.kotlin.kotlinp.Settings
import org.jetbrains.kotlin.kotlinp.jvm.JvmKotlinp
import org.jetbrains.kotlin.kotlinp.jvm.readKotlinClassHeader
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.FieldNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class ClassTask(
    private val checkerConfiguration: CheckerConfiguration,
    private val class1: ClassNode,
    private val class2: ClassNode,
    private val report: ClassReport,
) : Runnable {

    override fun run() {
        addClassInfo()
        checkMetadata()

        for (checker in checkerConfiguration.enabledClassCheckers) {
            checker.check(class1, class2, report)
        }

        checkMethods()
        checkFields()
    }

    private fun checkMetadata() {

        fun ClassNode.getMetadata(): KotlinClassMetadata? {
            val classWriter = ClassWriter(0)
            accept(classWriter)
            val classReader = ClassReader(classWriter.toByteArray())
            return classReader.readKotlinClassHeader()?.run { KotlinClassMetadata.readStrict(this) }
        }

        val kotlinp = JvmKotlinp(Settings(isVerbose = false, sortDeclarations = true))
        val metadata1 = class1.getMetadata()
        val metadata2 = class2.getMetadata()

        if (metadata1 == null && metadata2 == null) return

        if (metadata1 == null || metadata2 == null) {
            report.addMetadataDiff(
                ListEntryDiff(
                    metadata1?.run { kotlinp.printClassFile(metadata1) },
                    metadata2?.run { kotlinp.printClassFile(metadata2) })
            )
            return
        }

        if (metadata1::class != metadata2::class) {
            report.addMetadataDiff(ListEntryDiff(metadata1::class.simpleName, metadata2::class.simpleName))
            return
        }

        when (metadata1) {
            is KotlinClassMetadata.Class -> ClassMetadataTask(
                checkerConfiguration,
                metadata1,
                metadata2 as KotlinClassMetadata.Class,
                report.classMetadataReport()
            ).run()
            is KotlinClassMetadata.FileFacade -> PackageMetadataTask(
                checkerConfiguration,
                metadata1.kmPackage,
                (metadata2 as KotlinClassMetadata.FileFacade).kmPackage,
                report.fileFacadeMetadataReport()
            ).run()
            is KotlinClassMetadata.MultiFileClassFacade -> MultiFileClassFacadeMetadataTask(
                checkerConfiguration,
                metadata1,
                metadata2 as KotlinClassMetadata.MultiFileClassFacade,
                report.multiFileClassFacadeMetadataReport()
            )
            is KotlinClassMetadata.MultiFileClassPart -> MultiFileClassPartMetadataTask(
                checkerConfiguration,
                metadata1,
                metadata2 as KotlinClassMetadata.MultiFileClassPart,
                report.multiFileClassPartMetadataReport()
            )
            is KotlinClassMetadata.SyntheticClass -> SyntheticMetadataTask(
                checkerConfiguration,
                metadata1,
                metadata2 as KotlinClassMetadata.SyntheticClass,
                report.syntheticMetadataReport()
            )
            is KotlinClassMetadata.Unknown -> {}
        }
    }

    private fun addClassInfo() {
        report.info {
            tag("p") {
                tag("b", report.header1)
                println(": ${class1.access.classFlags()}")
            }
            tag("p") {
                tag("b", report.header2)
                println(": ${class2.access.classFlags()}")
            }
        }
    }

    private fun checkMethods() {
        val methods1 = class1.loadMethods()
        val methods2 = class2.loadMethods()

        val commonIds = methods1.keys.intersect(methods2.keys).sorted()
        for (id in commonIds) {
            val method1 = methods1[id]!!
            val method2 = methods2[id]!!
            if (method1.access.isSynthetic() && method2.access.isSynthetic()) continue
            val methodReport = report.methodReport(id)
            MethodTask(checkerConfiguration, method1, method2, methodReport).run()
        }
    }

    private fun checkFields() {
        val fields1 = class1.loadFields()
        val fields2 = class2.loadFields()

        val commonIds = fields1.keys.intersect(fields2.keys).sorted()
        for (id in commonIds) {
            val field1 = fields1[id]!!
            val field2 = fields2[id]!!
            if (field1.access.isSynthetic() && field2.access.isSynthetic()) continue
            val fieldReport = report.fieldReport(id)
            FieldTask(checkerConfiguration, field1, field2, fieldReport).run()
        }
    }
}

fun MethodNode.methodId() = "$name$desc"

fun FieldNode.fieldId() = "$name:$desc"
