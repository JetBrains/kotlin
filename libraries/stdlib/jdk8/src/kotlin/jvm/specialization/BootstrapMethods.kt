/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.specialization

import org.jetbrains.kotlin.codegen.util.inlinecodegen.JvmSpecializeMetadataValue
import org.jetbrains.kotlin.codegen.util.inlinecodegen.LightIrType
import org.jetbrains.kotlin.codegen.util.inlinecodegen.SpecTypeParametersUsages
import org.jetbrains.kotlin.codegen.util.inlinecodegen.SpecializedTypeAbi
import org.jetbrains.kotlin.codegen.util.inlinecodegen.extractJvmSpecializeMetadataValue
import org.jetbrains.kotlin.codegen.util.inlinecodegen.isSpecBootstrapCall
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.util.TraceClassVisitor
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.PrintWriter
import java.lang.invoke.*
import java.util.TreeSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

public object BootstrapMethods {
    private val cache = ConcurrentHashMap<String, MethodHandle>()
    private var counter = AtomicInteger(0)

    /**
     * @param lookup Call site lookup, provided by JVM
     * @param methodName The name of the specialized method
     * @param specializedMethodType The specialized method type
     * @param genericImplClass The class that contains the generic implementation of the specialized method
     * @param genericImplMethodType The method type of the generic implementation of the specialized method
     * @param specTypeParametersUsagesStr TODO
     * @param specializedTypeParametersStr TODO
     */
    @JvmStatic
    public fun bootstrapSpecializedGeneric(
        lookup: MethodHandles.Lookup,
        methodName: String,
        specializedMethodType: MethodType,
        genericImplClass: Class<*>,
        genericImplMethodType: MethodType,
        specTypeParametersUsagesStr: String, // needed for nested calls to avoid expensive reflection
        specializedTypeParametersStr: String,
    ): CallSite {
        val genericImplDesc = genericImplMethodType.toMethodDescriptorString()

        val cacheEntryName =
            System.identityHashCode(lookup.lookupClass().classLoader).toString() + "#" + // Lookup class loader
                    genericImplClass.name + "." + methodName + ":" + genericImplDesc + "#" + // Generic method
                    specializedTypeParametersStr // Type parameters

        cache[cacheEntryName]?.let { return ConstantCallSite(it) }

        val genericClassNode = readClassNode(genericImplClass)

        val genericMethodNode = genericClassNode.methods.find { it.name == methodName && it.desc == genericImplDesc }
            ?: throw RuntimeException("generic method not found: $methodName $genericImplDesc")

        val metadata = genericMethodNode.extractJvmSpecializeMetadataValue()
            ?: error("specialized method is missing the metadata annotation")

        val specializedClassNode = ClassNode().apply {
            this.name = lookup.lookupClass().name.replace('.', '/') + $$"$SpecializedClass$" + counter.incrementAndGet()
            this.version = genericClassNode.version
            this.superName = "java/lang/Object"
            this.access = Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL
            this.sourceFile = genericClassNode.sourceFile
            this.sourceDebug = genericClassNode.sourceDebug
        }

        val specializedMethodNode = MethodNode().apply {
            specializedClassNode.methods.add(this)
            this.name = "invoke"
            this.desc = specializedMethodType.toMethodDescriptorString()
            this.instructions.add(genericMethodNode.instructions)
            this.tryCatchBlocks = genericMethodNode.tryCatchBlocks
            this.localVariables = genericMethodNode.localVariables
            this.access = Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC + Opcodes.ACC_FINAL + Opcodes.ACC_SYNTHETIC
        }

        peeholeAdapt(
            specializedMethodNode,
            specializedMethodType.parameterArray(),
            LightIrType.decodeTypeParameters(specializedTypeParametersStr),
            metadata,
        )

        val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS).apply { specializedClassNode.accept(this) }
        val bytecode = classWriter.toByteArray()
        if (System.getenv("SPEC_DUMP_CLASS") != null) dumpClass(bytecode)

        val implHandle = defineClass(bytecode, specializedMethodType, lookup)
        cache[cacheEntryName] = implHandle
        return ConstantCallSite(implHandle)
    }
}

private fun readClassNode(clazz: Class<*>): ClassNode {
    val stream = clazz.classLoader.getResourceAsStream(clazz.name.replace('.', '/') + ".class")
        ?: error("could not get resource stream for class: ${clazz.name}")
    val node = ClassNode()
    ClassReader(readStream(stream)).accept(node, 0)
    return node
}

private fun readStream(inputStream: InputStream): ByteArray {
    inputStream.use { inputStream ->
        ByteArrayOutputStream().use { outputStream ->
            val tmpBuffer = ByteArray(1024 * 4)
            var bytesRead = 0
            var readCount = 0
            while ((inputStream.read(tmpBuffer, 0, tmpBuffer.size).also { bytesRead = it }) != -1) {
                outputStream.write(tmpBuffer, 0, bytesRead)
                readCount++
            }
            outputStream.flush()
            if (readCount == 1) return tmpBuffer
            return outputStream.toByteArray()
        }
    }
}

private fun defineClass(bytecode: ByteArray, specializedImplType: MethodType, callSiteLookup: MethodHandles.Lookup): MethodHandle {
    val javaVersion = System.getProperty("java.version")

    val lookup = when {
        javaVersion.startsWith("1.8.") -> defineClassJdk8(bytecode, callSiteLookup)
        else -> defineClassJdk11(bytecode, callSiteLookup)
    }

    return lookup.findStatic(lookup.lookupClass(), "invoke", specializedImplType)
}

private fun defineClassJdk8(bytecode: ByteArray, callSiteLookup: MethodHandles.Lookup): MethodHandles.Lookup {
    fun getUnsafeReflectively(unsafeClazz: Class<*>): Any? {
        val f = unsafeClazz.getDeclaredField("theUnsafe")
        f.setAccessible(true)
        return f.get(null)
    }

    val unsafeClazz = Class.forName("sun.misc.Unsafe")
    val unsafe = getUnsafeReflectively(unsafeClazz)
    val defineAnonymousClass = MethodHandles.lookup().findVirtual(
        unsafeClazz,
        "defineAnonymousClass",
        MethodType.methodType(Class::class.java, Class::class.java, ByteArray::class.java, Array::class.java)
    )
    val newClazz = defineAnonymousClass(unsafe, callSiteLookup.lookupClass(), bytecode, null) as Class<*>
    return callSiteLookup.`in`(newClazz)
}

private fun defineClassJdk11(bytecode: ByteArray, callSiteLookup: MethodHandles.Lookup): MethodHandles.Lookup {
    val defineClass = callSiteLookup.javaClass.getDeclaredMethod(
        "defineClass",
        ByteArray::class.java,
    )
    val newClazz = defineClass(callSiteLookup, bytecode) as Class<*>
    return callSiteLookup.`in`(newClazz)
}

private fun dumpClass(bytecode: ByteArray) {
    println("Dumping class: (${bytecode.size} bytes)")
    val classReader = ClassReader(bytecode)
    classReader.accept(TraceClassVisitor(PrintWriter(System.out)), 0)
}

private val Class<*>.width: Int
    get() = when (this) {
        Long::class.javaPrimitiveType, Double::class.javaPrimitiveType -> 2
        else -> 1
    }

private fun adaptLVT(
    methodNode: MethodNode,
    typeParameters: Map<Int, LightIrType>,
    metadata: JvmSpecializeMetadataValue,
    widenedSlots: WidenedSlots,
) {
    var lvtIndex = 0
    for ([varIndex, varNode] in methodNode.localVariables.withIndex()) {
        varNode.index = widenedSlots.adjustIndex(varNode.index)

        while (lvtIndex < metadata.specLVT.size && metadata.specLVT[lvtIndex].variableIndex < varIndex) lvtIndex++
        if (lvtIndex < metadata.specLVT.size && metadata.specLVT[lvtIndex].variableIndex == varIndex) {
            val specLocalVariable = metadata.specLVT[lvtIndex]
            SpecTypeParametersUsages.Usage(specLocalVariable.typeParameterIndex, specLocalVariable.isNullable)
                .adjustType(typeParameters)
                ?.let { SpecializedTypeAbi.fromLightIrType(it) }
                ?.also { abi -> varNode.desc = abi.reprDesc }
        }
    }
}

private fun peeholeAdapt(
    methodNode: MethodNode,
    specializedArgumentsTypes: Array<Class<*>>,
    typeParameters: Map<Int, LightIrType>,
    metadata: JvmSpecializeMetadataValue,
) {
    fun AbstractInsnNode.isIntrinsic(namePredicate: (String) -> Boolean): Boolean =
        this is MethodInsnNode &&
                this.opcode == Opcodes.INVOKESTATIC &&
                this.owner == "kotlin/jvm/internal/Intrinsics" &&
                namePredicate(this.name)

    fun AbstractInsnNode.isCheckNotNullParameter() = isIntrinsic { it == "checkNotNullParameter" }
    fun AbstractInsnNode.isSpecializedTypeDefaultValueMarker() = isIntrinsic { it.startsWith("specializedTypeDefaultValueMarker") }
    fun AbstractInsnNode.isSpecializedTypeMarker() = isIntrinsic { it.startsWith("specializedTypeMarker") }
    fun AbstractInsnNode.isBoxMarker() = isIntrinsic { it.startsWith("boxMarker") }
    fun AbstractInsnNode.isUnboxMarker() = isIntrinsic { it.startsWith("unboxMarker") }
    fun AbstractInsnNode.isCoerce2NullableMarker() = isIntrinsic { it.startsWith("coerce2NullableMarker") }
    fun AbstractInsnNode.isCoerce2NonNullableMarker() = isIntrinsic { it.startsWith("coerce2NonNullableMarker") }
    fun AbstractInsnNode.isReifiedOperationMarker() = isIntrinsic { it == "reifiedOperationMarker" }

    // local variable index -> parameter index
    val varIxdToParamIdx = specializedArgumentsTypes
        .runningFold(0) { acc, ty -> acc + ty.width }
        .dropLast(1)
        .withIndex()
        .associate { [i, offset] -> offset to i }

    val typeParametersAbi = buildMap {
        for ([k, v] in typeParameters) {
            SpecializedTypeAbi.fromLightIrType(v)?.also { put(k, it) }
        }
    }

    val instructions = methodNode.instructions
    val widenedSlots = metadata.calcWidenedSlots(typeParametersAbi)

    adaptLVT(methodNode, typeParameters, metadata, widenedSlots)

    for (insn in instructions.toArray()) {
        when {
            insn.isCheckNotNullParameter() -> {
                val prev = insn.previous ?: continue
                val prev2 = prev.previous ?: continue
                if (prev2 !is VarInsnNode) continue
                if (prev !is LdcInsnNode) continue
                if (specializedArgumentsTypes[varIxdToParamIdx[prev2.`var`]!!].isPrimitive) {
                    instructions.set(insn, InsnNode(Opcodes.NOP))
                    instructions.set(prev, InsnNode(Opcodes.NOP))
                    instructions.set(prev2, InsnNode(Opcodes.NOP))
                }
            }

            insn.isSpecializedTypeDefaultValueMarker() -> {
                val typeParameterIndex = (insn as MethodInsnNode).name.substring("specializedTypeDefaultValueMarker".length).toInt()
                instructions.set(insn, InsnNode(typeParametersAbi[typeParameterIndex]?.defaultOpcode ?: Opcodes.ACONST_NULL))
            }

            insn is VarInsnNode -> {
                insn.`var` = widenedSlots.adjustIndex(insn.`var`)

                if (insn.opcode == Opcodes.ALOAD || insn.opcode == Opcodes.ASTORE) {
                    insn.previous?.takeIf { it.isSpecializedTypeMarker() }?.let { prev ->
                        val typeParameterUsage =
                            SpecTypeParametersUsages.Usage.decode((prev as MethodInsnNode).name.substring("specializedTypeMarker".length))
                        instructions.set(prev, InsnNode(Opcodes.NOP))
                        typeParameterUsage.adjustType(typeParameters)
                            ?.let { SpecializedTypeAbi.fromLightIrType(it) }
                            ?.let { instructions.set(insn, VarInsnNode(insn.opcode - 4 + it.loadStoreReturnOpcodeOffset, insn.`var`)) }
                    }
                }
            }

            insn.opcode == Opcodes.ARETURN -> {
                metadata.specTypeParametersUsages.returnType?.adjustType(typeParameters)
                    ?.let { SpecializedTypeAbi.fromLightIrType(it) }
                    ?.let { abi -> instructions.set(insn, InsnNode(Opcodes.IRETURN + abi.loadStoreReturnOpcodeOffset)) }
            }

            insn.isBoxMarker() -> {
                val typeParameterUsage =
                    SpecTypeParametersUsages.Usage.decode((insn as MethodInsnNode).name.substring("boxMarker".length))
                when (val abi = typeParameterUsage.adjustType(typeParameters)?.let { SpecializedTypeAbi.fromLightIrType(it) }) {
                    null -> instructions.set(insn, InsnNode(Opcodes.NOP))
                    else -> abi.genBox(instructions, insn)
                }
            }

            insn.isUnboxMarker() -> {
                val typeParameterUsage =
                    SpecTypeParametersUsages.Usage.decode((insn as MethodInsnNode).name.substring("unboxMarker".length))
                when (val abi = typeParameterUsage.adjustType(typeParameters)?.let { SpecializedTypeAbi.fromLightIrType(it) }) {
                    null -> instructions.set(insn, InsnNode(Opcodes.NOP))
                    else -> abi.genUnbox(instructions, insn)
                }
            }

            insn.isCoerce2NullableMarker() -> {
                val typeParameterIndex = (insn as MethodInsnNode).name.substring("coerce2NullableMarker".length).toInt()
                when (val abi = typeParameters[typeParameterIndex]?.let { SpecializedTypeAbi.fromLightIrType(it) }) {
                    null -> instructions.set(insn, InsnNode(Opcodes.NOP))
                    else -> abi.genCoerce2Nullable(instructions, insn)
                }
            }

            insn.isCoerce2NonNullableMarker() -> {
                val typeParameterIndex = (insn as MethodInsnNode).name.substring("coerce2NonNullableMarker".length).toInt()
                when (val abi = typeParameters[typeParameterIndex]?.let { SpecializedTypeAbi.fromLightIrType(it) }) {
                    null -> instructions.set(insn, InsnNode(Opcodes.NOP))
                    else -> abi.genCoerce2NonNullable(instructions, insn)
                }
            }

            insn.isReifiedOperationMarker() -> reify(methodNode, insn as MethodInsnNode, metadata.typeParametersNames, typeParameters)

            insn is InvokeDynamicInsnNode && insn.isSpecBootstrapCall -> {
                val mapping = buildMap {
                    typeParameters.forEach { (genericIndex, typeParameterValue) ->
                        put(metadata.typeParametersNames[genericIndex], typeParameterValue)
                    }
                }
                val nestedSpecTypeParametersUsages = SpecTypeParametersUsages.decode(insn.bsmArgs[2] as String)
                val nestedTypeParameters = LightIrType.decodeTypeParameters(insn.bsmArgs[3] as String).mapValues { it.value.reify(mapping) }
                val descArgs = Type.getArgumentTypes(insn.desc)
                var descReturnType = Type.getReturnType(insn.desc)
                for ((parameterIndex, usage) in nestedSpecTypeParametersUsages.parameterGenericIndices) {
                    usage.adjustType(nestedTypeParameters)
                        ?.let { SpecializedTypeAbi.fromLightIrType(it) }
                        ?.also { descArgs[parameterIndex] = Type.getType(it.reprDesc) }
                }
                nestedSpecTypeParametersUsages.returnType
                    ?.adjustType(nestedTypeParameters)
                    ?.let { SpecializedTypeAbi.fromLightIrType(it) }
                    ?.also { descReturnType = Type.getType(it.reprDesc) }
                insn.desc = Type.getMethodType(descReturnType, *descArgs).descriptor
                insn.bsmArgs[3] = nestedTypeParameters.entries.joinToString("\n") { (k, v) -> "$k=${v.encode()}" }
            }
        }
    }

    instructions.removeAll { it.opcode == Opcodes.NOP }
}

@JvmInline
private value class WidenedSlots(val slots: List<Int>) {
    fun adjustIndex(index: Int): Int {
        var index = index
        for ([slotIndex, slot] in slots.withIndex()) {
            if (index > slot + slotIndex) {
                index += 1
            } else {
                break
            }
        }
        return index
    }
}

private fun JvmSpecializeMetadataValue.calcWidenedSlots(specializedTypeParameters: Map<Int, SpecializedTypeAbi>): WidenedSlots {
    val slots = TreeSet<Int>()
    var index = 0
    while (index < specializedSlots.size) {
        val genericIndex = specializedSlots[index++]
        val size = specializedSlots[index++]
        if (specializedTypeParameters[genericIndex]?.isWide == true) {
            repeat(size) { slots.add(specializedSlots[index++]) }
        } else {
            index += size
        }
    }
    return WidenedSlots(slots.toList())
}
