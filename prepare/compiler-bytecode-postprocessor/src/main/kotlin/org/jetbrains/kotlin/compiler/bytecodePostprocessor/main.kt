package org.jetbrains.kotlin.compiler.bytecodePostprocessor

import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.and
import kotlin.inv
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.or
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.text.toLong

private val allClasses = mutableMapOf<String, ClassInfo>()

fun main(args: Array<String>) {
    val (inputJarFileName, classPathDense, outputFileName, processClassPatternsDense) = args

    loadAllClasses(
        inputJarFileName,
        classPathDense.split(';'),
        processClassPatternsDense.let { if (it.isEmpty()) emptyList() else it.split(';') }
    )

    val processedClasses = mutableSetOf<ClassInfo>()
    for (clazz in allClasses.values) {
        processClass(clazz, processedClasses)
    }

    saveResult(outputFileName)
}


private fun loadAllClasses(inputJarFileName: String, classPath: List<String>, processClassPatterns: List<String>) {
    val includeClassPatterns = processClassPatterns.map { it.trimEnd('*') }
    ZipInputStream(Path(inputJarFileName).inputStream().buffered()).use { inputJar ->
        while (true) {
            val entry = inputJar.getNextEntry() ?: break
            if (entry.isDirectory) continue
            if (entry.name.endsWith(".class")) {
                val internalName = entry.name.removeSuffix(".class")
                val packageName = internalName.replace('/', '.')

                val include = includeClassPatterns.isEmpty() || includeClassPatterns.any { pattern ->
                    if (pattern.endsWith('.')) packageName.startsWith(pattern)
                    else packageName == pattern
                }

                var clazz: ClassNode? = null
                if (include) {
                    val cr = ClassReader(inputJar.buffered())
                    clazz = ClassNode()
                    cr.accept(clazz, 0)
                }

                val classInfo = ClassInfo(internalName, include, clazz, entry)
                allClasses[internalName] = classInfo

                if (clazz != null) {
                    clazz.methods.forEach { it.declaringClass = classInfo }
                }
            }

            inputJar.closeEntry()
        }
    }

    markSubclasses()
}

private fun markSubclasses() {
    for (clazzInfo in allClasses.values) {
        clazzInfo.classNode?.let { clazz ->
            for (superName in clazz.superTypeNames) {
                allClasses[superName]?.directSubclasses += clazzInfo
            }
        }
    }
}

private fun saveResult(outputFileName: String) {
    ZipOutputStream(Path(outputFileName).outputStream().buffered()).use { outputStream ->
        for (clazz in allClasses.values) {
            if (!clazz.isApplicationClass) continue

            val cw = ClassWriter(0)
            clazz.classNode!!.accept(cw)
            val binary = cw.toByteArray()

            val zipEntry = ZipEntry(clazz.zipEntry.name).apply {
                size = binary.size.toLong()
                method = clazz.zipEntry.method
            }
            outputStream.putNextEntry(zipEntry)
            outputStream.write(binary)
            outputStream.closeEntry()
        }
    }
}

private fun processClass(clazz: ClassInfo, processedClasses: MutableSet<ClassInfo>) {
    if (!processedClasses.add(clazz)) return
    val clazzNode = clazz.classNode ?: return

    if (clazzNode.access and Opcodes.ACC_INTERFACE != 0 || clazzNode.access and Opcodes.ACC_FINAL != 0) return
    val subClasses = clazz.directSubclasses
    if (subClasses.size < 1) return
    if (!subClasses.all { it.isApplicationClass }) return

    for (subClass in subClasses) {
        processClass(subClass, processedClasses)
    }

    val hoistableFields = subClasses.flatMap { it.classNode!!.fields }
        .filterNot { it.access and Opcodes.ACC_STATIC != 0 }
        //.filter { it.isPrivate || it.isProtected }
        .groupBy { it.name }
        .filterValues { it.size == subClasses.size }
        .filterValues { fields -> fields.map { it.desc }.distinct().size == 1 }

    val allMethods = mutableMapOf<String, MethodNode>()
    fun collect(ancestor: ClassInfo) {
        if (ancestor !== clazz && !ancestor.isApplicationClass) return
        val ancestorNode = ancestor.classNode!!
        ancestorNode.methods.forEach {
            allMethods.putIfAbsent(it.name + it.desc, it)
        }
        ancestorNode.superTypeNames
            .mapNotNull { allClasses[it] }
            .forEach { collect(it) }
    }
    collect(clazz)

    val hoistedFields = mutableMapOf<String, FieldNode>()
    for (baseMethod in allMethods.values) {
        tryHoistMethod(clazz, baseMethod, subClasses, hoistableFields.keys, hoistedFields)
    }

    for (name in hoistedFields.keys) {
        for (subClass in subClasses) {
            subClass.classNode!!.fields.removeIf { it.name == name }
        }
    }
}

private fun tryHoistMethod(
    clazz: ClassInfo,
    baseMethod: MethodNode,
    subClasses: List<ClassInfo>,
    hoistableFields: Set<String>,
    hoistedFields: MutableMap<String, FieldNode>,
) {
    if (baseMethod.access and Opcodes.ACC_ABSTRACT == 0 || baseMethod.access and Opcodes.ACC_PRIVATE != 0) return

    val implMethods = subClasses.mapNotNull { subClass ->
        subClass.classNode!!.methods.singleOrNull { it.name == baseMethod.name && it.desc == baseMethod.desc }
    }
    if (implMethods.size != subClasses.size || !implMethods.all { it.access and Opcodes.ACC_ABSTRACT == 0 }) return

    var firstImpl: MethodNode? = null
    for (impl in implMethods) {
        if (firstImpl == null) {
            firstImpl = impl
        } else if (!compareMethods(firstImpl, impl)) {
            return
        }
    }

    if (!checkCanHoist(firstImpl!!, clazz, hoistableFields)) {
        return
    }
    println(
        "Hoist method ${clazz.className}.${baseMethod.name.substringAfterLast('.')} from " +
                "[${subClasses.joinToString { it.className.substringAfterLast('.') }}]"
    )

    hoistMethod(firstImpl, baseMethod, clazz, implMethods, hoistableFields, hoistedFields)
    for ((subClass, impl) in subClasses zip implMethods) {
        (subClass.classNode!!.methods as MutableList) -= impl
    }
}

private fun compareMethods(aMethod: MethodNode, bMethod: MethodNode): Boolean {
    val aInstructions = aMethod.instructions
    val bInstructions = bMethod.instructions

    infix fun LabelNode.equals(other: LabelNode): Boolean =
        aInstructions.indexOf(this) == bInstructions.indexOf(other)

    fun Iterable<AbstractInsnNode>.filterReal() = asSequence().filterNot { it is LabelNode || it is LineNumberNode || it is FrameNode }
    for ((a, b) in aInstructions.filterReal() zip bInstructions.filterReal()) {
        if (a.opcode != b.opcode) return false
        when (a) {
            is LabelNode, is LineNumberNode, is FrameNode -> {}
            is InsnNode -> {}
            is VarInsnNode -> {
                b as VarInsnNode
                if (a.`var` != b.`var`) return false
            }
            is IntInsnNode -> {
                b as IntInsnNode
                if (a.operand != b.operand) return false
            }
            is IincInsnNode -> {
                b as IincInsnNode
                if (a.`var` != b.`var`) return false
                if (a.incr != b.incr) return false
            }
            is TypeInsnNode -> {
                b as TypeInsnNode
                if (a.desc != b.desc) return false
            }
            is MultiANewArrayInsnNode -> {
                b as MultiANewArrayInsnNode
                if (a.desc != b.desc) return false
                if (a.dims != b.dims) return false
            }
            is LdcInsnNode -> {
                b as LdcInsnNode
                if (a.cst != b.cst) return false
            }
            is JumpInsnNode -> {
                b as JumpInsnNode
                if (!(a.label equals b.label)) return false
            }
            is TableSwitchInsnNode -> {
                b as TableSwitchInsnNode
                if (a.min != b.min) return false
                if (a.max != b.max) return false
                if ((a.labels zip b.labels).all { it.first equals it.second }) return false
                if (!(a.dflt equals b.dflt)) return false
            }
            is LookupSwitchInsnNode -> {
                b as LookupSwitchInsnNode
                if (a.keys != b.keys) return false
                if ((a.labels zip b.labels).all { it.first equals it.second }) return false
                if (!(a.dflt equals b.dflt)) return false
            }
            is MethodInsnNode -> {
                b as MethodInsnNode
                if (a.owner != b.owner) return false
                if (a.name != b.name) return false
                if (a.desc != b.desc) return false
            }
            is InvokeDynamicInsnNode -> {
                b as InvokeDynamicInsnNode
                if (a.name != b.name) return false
                if (a.desc != b.desc) return false
                if (a.bsm != b.bsm) return false
                if (a.bsmArgs != b.bsmArgs) return false
            }
            is FieldInsnNode -> {
                b as FieldInsnNode
                if (a.opcode == Opcodes.GETFIELD || a.opcode == Opcodes.PUTFIELD) {
                    if (a.name != b.name) return false
                    if (a.desc != b.desc) return false
                    if (a.owner != b.owner) {
                        if (a.owner != aMethod.declaringClass.name ||
                            b.owner != bMethod.declaringClass.name
                        ) return false
                    }
                } else {
                    if (a.owner != b.owner) return false
                    if (a.name != b.name) return false
                    if (a.desc != b.desc) return false
                }
            }
        }
    }

    return true
}

private fun checkCanHoist(source: MethodNode, targetClass: ClassInfo, hoistableFields: Set<String>): Boolean {
    val instructions = source.instructions
    //if (instructions.size() > 10) return false

    for (inst in instructions) {
        when (inst) {
            is TypeInsnNode -> {
                //val type = inst.desc.trimStart('[')
                //if (!hierarchy.isVisible(targetClass, type.sootClass)) return false
            }
            is MultiANewArrayInsnNode -> {
                //val type = inst.baseType.baseType
                //if (type is RefType && !hierarchy.isVisible(targetClass, type.sootClass)) return false
            }
            is InvokeDynamicInsnNode -> return false
            is MethodInsnNode -> {
                if (inst.owner == source.declaringClass.name) return false
                //if (!hierarchy.isVisible(targetClass, inst.method)) return false
            }
            is FieldInsnNode -> {
                if (inst.opcode == Opcodes.GETFIELD || inst.opcode == Opcodes.PUTFIELD) {
                    if (inst.owner == source.declaringClass.name) {
                        if (inst.name !in hoistableFields) return false
                    } else {
                        //if (!hierarchy.isVisible(targetClass, inst.field)) return false
                    }
                } else {
                    //if (!hierarchy.isVisible(targetClass, inst.field)) return false
                }
            }
        }
    }

    return true
}


private fun hoistMethod(
    source: MethodNode,
    baseMethod: MethodNode,
    targetClass: ClassInfo,
    allSourceMethods: List<MethodNode>,
    hoistableFields: Set<String>,
    hoistedFields: MutableMap<String, FieldNode>,
) {
    val target: MethodNode
    if (baseMethod.declaringClass == targetClass) {
        target = baseMethod
    } else {
        target = MethodNode(baseMethod.access, baseMethod.name, baseMethod.desc, baseMethod.signature, baseMethod.exceptions.toTypedArray())
        targetClass.classNode!!.methods.add(target)
        target.declaringClass = targetClass
    }

    target.access = target.access and Opcodes.ACC_ABSTRACT.inv()
    if (allSourceMethods.all { it.access and Opcodes.ACC_FINAL != 0 || it.access and Opcodes.ACC_FINAL != 0 }) { // condition may be better
        target.access = target.access or Opcodes.ACC_FINAL
    }

    val sourceBody = source.instructions
    val targetBody = target.instructions
    target.maxStack = source.maxStack
    target.maxLocals = source.maxLocals

    val clonedLabels = buildMap {
        for (inst in sourceBody) {
            if(inst is LabelNode) {
                put(inst, LabelNode(Label()))
            }
        }
    }
    for (inst in sourceBody) {
        inst.clone(clonedLabels)?.let {
            if (it !is LineNumberNode) {
                targetBody.add(it)
            }
        }
    }

    for (inst in targetBody) {
        if (inst.opcode == Opcodes.GETFIELD || inst.opcode == Opcodes.PUTFIELD) {
            inst as FieldInsnNode
            if (inst.owner == source.declaringClass.name) {
                check(inst.name in hoistableFields) { "Field ${inst.name} cannot be hoisted" }

                val field = inst.resolve()
                val newField = hoistedFields.computeIfAbsent(inst.name) {
                    val access = mergeModifierVisibilities(field.access, Opcodes.ACC_PROTECTED)
                    FieldNode(access, field.name, field.desc, field.signature, field.value).also {
                        targetClass.classNode!!.fields.add(it)
                    }
                }

                newField.access = mergeModifierVisibilities(newField.access, field.access)
                if (true /*!field.isFinal*/) {
                    // Final fields cannot be written from a subclass
                    newField.access = newField.access and Opcodes.ACC_FINAL.inv()
                }

                inst.owner = targetClass.name
            }
        }
    }
}

private fun mergeModifierVisibilities(modifiers: Int, visibility: Int): Int {
    val AllVisibilities = Opcodes.ACC_PRIVATE or Opcodes.ACC_PROTECTED or Opcodes.ACC_PUBLIC
    val otherModifiers = modifiers and AllVisibilities.inv()
    return when {
        modifiers and Opcodes.ACC_PUBLIC != 0 || visibility and Opcodes.ACC_PUBLIC != 0 -> otherModifiers or Opcodes.ACC_PUBLIC
        modifiers and AllVisibilities == 0 || visibility and AllVisibilities == 0 -> otherModifiers
        modifiers and Opcodes.ACC_PROTECTED != 0 || visibility and Opcodes.ACC_PROTECTED != 0 -> otherModifiers or Opcodes.ACC_PROTECTED
        else -> otherModifiers or Opcodes.ACC_PRIVATE
    }
}

private class ClassInfo(
    val name: String,
    val isApplicationClass: Boolean,
    val classNode: ClassNode?,
    val zipEntry: ZipEntry,
) {
    val directSubclasses = mutableListOf<ClassInfo>()
    val type: Type get() = Type.getObjectType(name)
    val className: String get() = type.className

    override fun toString(): String = name
}

private val ClassNode.superTypeNames: List<String?>
    get() = listOfNotNull(superName) + interfaces

private var MethodNode.declaringClass: ClassInfo by object : ReadWriteProperty<MethodNode, ClassInfo> {
    private val map = hashMapOf<MethodNode, ClassInfo>()
    override fun getValue(thisRef: MethodNode, property: KProperty<*>): ClassInfo = map.getValue(thisRef)
    override fun setValue(thisRef: MethodNode, property: KProperty<*>, value: ClassInfo) {
        map[thisRef] = value
    }
}

private fun FieldInsnNode.resolve() =
    allClasses.getValue(owner).classNode!!.fields.first { it.name == name }
