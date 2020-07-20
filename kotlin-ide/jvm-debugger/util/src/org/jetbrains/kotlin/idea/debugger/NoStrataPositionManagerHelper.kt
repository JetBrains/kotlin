/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcess
import com.intellij.debugger.jdi.VirtualMachineProxyImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.containers.ConcurrentFactoryMap
import com.sun.jdi.Location
import com.sun.jdi.ReferenceType
import com.sun.jdi.VirtualMachine
import org.jetbrains.kotlin.codegen.inline.SMAP
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.util.getLineCount
import org.jetbrains.kotlin.idea.core.util.getLineStartOffset
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.org.objectweb.asm.*
import java.util.*
import java.util.concurrent.ConcurrentMap

fun isInlineFunctionLineNumber(file: VirtualFile, lineNumber: Int, project: Project): Boolean {
    if (ProjectRootsUtil.isProjectSourceFile(project, file)) {
        val linesInFile = file.toPsiFile(project)?.getLineCount() ?: return false
        return lineNumber > linesInFile
    }

    return true
}

fun createWeakBytecodeDebugInfoStorage(): ConcurrentMap<BinaryCacheKey, SMAP?> {
    return ConcurrentFactoryMap.createWeakMap<BinaryCacheKey, SMAP?> { key ->
        val bytes = ClassBytecodeFinder(key.project, key.jvmName, key.file).find() ?: return@createWeakMap null
        return@createWeakMap readDebugInfo(bytes)
    }
}

data class BinaryCacheKey(val project: Project, val jvmName: JvmClassName, val file: VirtualFile)

fun getLocationsOfInlinedLine(type: ReferenceType, position: SourcePosition, sourceSearchScope: GlobalSearchScope): List<Location> {
    val line = position.line
    val file = position.file
    val project = position.file.project

    val lineStartOffset = file.getLineStartOffset(line) ?: return listOf()
    val element = file.findElementAt(lineStartOffset) ?: return listOf()
    val ktElement = element.parents.firstIsInstanceOrNull<KtElement>() ?: return listOf()

    val isInInline = runReadAction { element.parents.any { it is KtFunction && it.hasModifier(KtTokens.INLINE_KEYWORD) } }

    if (!isInInline) {
        // Lambdas passed to crossinline arguments are inlined when they are used in non-inlined lambdas
        val isInCrossinlineArgument = isInCrossinlineArgument(ktElement)
        if (!isInCrossinlineArgument) {
            return listOf()
        }
    }

    val lines = inlinedLinesNumbers(line + 1, position.file.name, FqName(type.name()), type.sourceName(), project, sourceSearchScope)

    return lines.flatMap { type.locationsOfLine(it) }
}

fun isInCrossinlineArgument(ktElement: KtElement): Boolean {
    val argumentFunctions = runReadAction {
        ktElement.parents.filter {
            when (it) {
                is KtFunctionLiteral -> it.parent is KtLambdaExpression &&
                        (it.parent.parent is KtValueArgument || it.parent.parent is KtLambdaArgument)
                is KtFunction -> it.parent is KtValueArgument
                else -> false
            }
        }.filterIsInstance<KtFunction>()
    }

    val bindingContext = ktElement.analyze(BodyResolveMode.PARTIAL)
    return argumentFunctions.any {
        val argumentDescriptor = InlineUtil.getInlineArgumentDescriptor(it, bindingContext)
        argumentDescriptor?.isCrossinline ?: false
    }
}


private fun inlinedLinesNumbers(
    inlineLineNumber: Int, inlineFileName: String,
    destinationTypeFqName: FqName, destinationFileName: String,
    project: Project, sourceSearchScope: GlobalSearchScope
): List<Int> {
    val internalName = destinationTypeFqName.asString().replace('.', '/')
    val jvmClassName = JvmClassName.byInternalName(internalName)

    val file = DebuggerUtils.findSourceFileForClassIncludeLibrarySources(project, sourceSearchScope, jvmClassName, destinationFileName)
        ?: return listOf()

    val virtualFile = file.virtualFile ?: return listOf()

    val smapData = KotlinDebuggerCaches.getSmapCached(project, jvmClassName, virtualFile) ?: return listOf()

    val mappingsToInlinedFile = smapData.fileMappings.filter { it.name == inlineFileName }
    val mappingIntervals = mappingsToInlinedFile.flatMap { it.lineMappings }

    return mappingIntervals.asSequence().filter { rangeMapping -> rangeMapping.hasMappingForSource(inlineLineNumber) }
        .map { rangeMapping -> rangeMapping.mapSourceToDest(inlineLineNumber) }.filter { line -> line != -1 }.toList()
}

@Volatile
var emulateDexDebugInTests: Boolean = false

fun DebugProcess.isDexDebug(): Boolean {
    val virtualMachine = (this.virtualMachineProxy as? VirtualMachineProxyImpl)?.virtualMachine
    return virtualMachine.isDexDebug()
}

fun VirtualMachine?.isDexDebug(): Boolean {
    // TODO: check other machine names
    return (emulateDexDebugInTests && ApplicationManager.getApplication().isUnitTestMode) || this?.name() == "Dalvik"
}
