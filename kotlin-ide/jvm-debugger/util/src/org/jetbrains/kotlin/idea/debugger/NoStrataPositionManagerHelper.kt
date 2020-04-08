/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcess
import com.intellij.debugger.jdi.VirtualMachineProxyImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.compiler.CompilerPaths
import com.intellij.openapi.compiler.ex.CompilerPathsEx
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.containers.ConcurrentFactoryMap
import com.sun.jdi.Location
import com.sun.jdi.ReferenceType
import com.sun.jdi.VirtualMachine
import org.jetbrains.kotlin.idea.caches.project.implementingModules
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.util.getLineCount
import org.jetbrains.kotlin.idea.core.util.getLineStartOffset
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.tail
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.getOrPutNullable
import org.jetbrains.org.objectweb.asm.*
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentMap

fun isInlineFunctionLineNumber(file: VirtualFile, lineNumber: Int, project: Project): Boolean {
    if (ProjectRootsUtil.isProjectSourceFile(project, file)) {
        val linesInFile = file.toPsiFile(project)?.getLineCount() ?: return false
        return lineNumber > linesInFile
    }

    return true
}

fun readBytecodeInfo(
    project: Project,
    jvmName: JvmClassName,
    file: VirtualFile
): BytecodeDebugInfo? {
    return KotlinDebuggerCaches.getOrReadDebugInfoFromBytecode(project, jvmName, file)
}

fun createWeakBytecodeDebugInfoStorage(): ConcurrentMap<BinaryCacheKey, BytecodeDebugInfo?> {
    return ConcurrentFactoryMap.createWeakMap<BinaryCacheKey, BytecodeDebugInfo?> { key ->
        val bytes = readClassFileImpl(key.project, key.jvmName, key.file) ?: return@createWeakMap null

        val smapData = readDebugInfo(bytes)
        val lineNumberMapping = readLineNumberTableMapping(bytes)

        BytecodeDebugInfo(smapData, lineNumberMapping)
    }
}

class BytecodeDebugInfo(val smapData: SmapData?, val lineTableMapping: Map<BytecodeMethodKey, Map<String, Set<Int>>>)

data class BytecodeMethodKey(val methodName: String, val signature: String)

data class BinaryCacheKey(val project: Project, val jvmName: JvmClassName, val file: VirtualFile)

private fun readClassFileImpl(
    project: Project,
    jvmName: JvmClassName,
    file: VirtualFile
): ByteArray? {
    val fqNameWithInners = jvmName.fqNameForClassNameWithoutDollars.tail(jvmName.packageFqName)

    fun readFromLibrary(): ByteArray? {
        if (!ProjectRootsUtil.isLibrarySourceFile(project, file)) return null

        val classId = ClassId(jvmName.packageFqName, Name.identifier(fqNameWithInners.asString()))

        // TODO use debugger search scope
        val fileFinder = VirtualFileFinderFactory.getInstance(project).create(GlobalSearchScope.allScope(project))
        val classFile = fileFinder.findVirtualFileWithHeader(classId) ?: return null
        return classFile.contentsToByteArray(false)
    }

    fun readFromOutput(isForTestClasses: Boolean): ByteArray? {
        fun readFromOutputOfModule(module: Module): File? {
            val outputPaths = CompilerPathsEx.getOutputPaths(arrayOf(module)).toList()
            val className = fqNameWithInners.asString().replace('.', '$')
            var classFile = findClassFileByPaths(jvmName.packageFqName.asString(), className, outputPaths)

            if (classFile == null) {
                if (!isForTestClasses) {
                    return null
                }

                val outputDir = CompilerPaths.getModuleOutputDirectory(module, /*forTests = */ isForTestClasses) ?: return null

                val outputModeDirName = outputDir.name
                // FIXME: It looks like this doesn't work anymore after Kotlin gradle plugin have stopped generating Kotlin classes in java output dir
                // Originally this code did mapping like 'path/classes/test/debug' -> 'path/classes/androidTest/debug'
                val androidTestOutputDir = outputDir.parent?.parent?.findChild("androidTest")?.findChild(outputModeDirName) ?: return null

                classFile = findClassFileByPath(jvmName.packageFqName.asString(), className, androidTestOutputDir.path) ?: return null
            }
            return classFile
        }

        if (!ProjectRootsUtil.isProjectSourceFile(project, file)) return null

        val module = ProjectFileIndex.SERVICE.getInstance(project).getModuleForFile(file) ?: return null

        val classFile = readFromOutputOfModule(module)
        return if (classFile == null) {
            for (implementing in module.implementingModules) {
                readFromOutputOfModule(implementing)?.let { return it.readBytes() }
            }
            null
        } else {
            classFile.readBytes()
        }
    }

    fun readFromSourceOutput(): ByteArray? = readFromOutput(false)

    fun readFromTestOutput(): ByteArray? = readFromOutput(true)

    return readFromLibrary() ?: readFromSourceOutput() ?: readFromTestOutput()
}

private fun findClassFileByPaths(packageName: String, className: String, paths: List<String>): File? =
    paths.mapNotNull { path -> findClassFileByPath(packageName, className, path) }.maxBy { it.lastModified() }

private fun findClassFileByPath(packageName: String, className: String, outputDirPath: String): File? {
    val outDirFile = File(outputDirPath).takeIf(File::exists) ?: return null

    val parentDirectory = File(outDirFile, packageName.replace(".", File.separator))
    if (!parentDirectory.exists()) return null

    if (ApplicationManager.getApplication().isUnitTestMode) {
        val beforeDexFileClassFile = File(parentDirectory, className + ".class.before_dex")
        if (beforeDexFileClassFile.exists()) {
            return beforeDexFileClassFile
        }
    }

    val classFile = File(parentDirectory, className + ".class")
    if (classFile.exists()) {
        return classFile
    }

    return null
}

private fun readLineNumberTableMapping(bytes: ByteArray): Map<BytecodeMethodKey, Map<String, Set<Int>>> {
    val lineNumberMapping = HashMap<BytecodeMethodKey, Map<String, Set<Int>>>()

    ClassReader(bytes).accept(object : ClassVisitor(Opcodes.API_VERSION) {
        override fun visitMethod(
            access: Int,
            name: String?,
            desc: String?,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor? {
            if (name == null || desc == null) {
                return null
            }

            val methodKey = BytecodeMethodKey(name, desc)
            val methodLinesMapping = HashMap<String, MutableSet<Int>>()
            lineNumberMapping[methodKey] = methodLinesMapping

            return object : MethodVisitor(Opcodes.API_VERSION, null) {
                override fun visitLineNumber(line: Int, start: Label?) {
                    if (start != null) {
                        methodLinesMapping.getOrPutNullable(start.toString(), { LinkedHashSet<Int>() }).add(line)
                    }
                }
            }
        }
    }, ClassReader.SKIP_FRAMES and ClassReader.SKIP_CODE)

    return lineNumberMapping
}

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

    val debugInfo = readBytecodeInfo(project, jvmClassName, virtualFile) ?: return listOf()
    val smapData = debugInfo.smapData ?: return listOf()

    val smap = smapData.kotlinStrata ?: return listOf()

    val mappingsToInlinedFile = smap.fileMappings.filter { it.name == inlineFileName }
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