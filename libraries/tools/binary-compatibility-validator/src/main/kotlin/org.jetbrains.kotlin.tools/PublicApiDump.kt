package org.jetbrains.kotlin.tools

import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import java.io.File
import java.io.InputStream
import java.util.jar.JarFile
import kotlin.comparisons.compareBy

fun main(args: Array<String>) {
    var src = args[0]
    println(src)
    println("------------------\n");
    val visibilities = readKotlinVisibilities(File("""stdlib/target/stdlib-declarations.json"""))
    getBinaryAPI(JarFile(src), visibilities).filterOutNonPublic().dump()
}


fun JarFile.classEntries() = entries().asSequence().filter { !it.isDirectory && it.name.endsWith(".class") }


data class ClassBinarySignature(val name: String, val superName: String, val outerName: String?, val modifiers: String, val supertypes: List<String>, val memberSignatures: List<MemberBinarySignature>, val isPublic: Boolean)
data class MemberBinarySignature(val name: String, val signature: String, val isStatic: Boolean)

val ClassBinarySignature.signature: String
    get() = "$modifiers class $name" + if (supertypes.isEmpty()) "" else ": ${supertypes.joinToString()}"


fun getBinaryAPI(jar: JarFile, visibilityMap: Map<String, ClassVisibility>): List<ClassBinarySignature> =
        getBinaryAPI(jar.classEntries().map { entry -> jar.getInputStream(entry) }, visibilityMap)

fun getBinaryAPI(classStreams: Sequence<InputStream>, visibilityMap: Map<String, ClassVisibility>): List<ClassBinarySignature> =
        classStreams.map { it.use { stream ->
                val classNode = ClassNode()
                ClassReader(stream).accept(classNode, ClassReader.SKIP_CODE)
                classNode
            }
        }
        .asIterable()
        .sortedBy { it.name }
        .mapNotNull { with(it) {
            val classVisibility = visibilityMap[name]
            val isPublic = it.isEffectivelyPublic(classVisibility)

            val supertypes = listOf(superName) - "java/lang/Object" + interfaces.sorted()

            val modifiers = getModifierString(effectiveAccess and Opcodes.ACC_STATIC.inv())

            val memberSignatures =
                    fields.filter { it.isEffectivelyPublic(classVisibility) }
                            .sortedBy { it.name }
                            .map { with(it) { MemberBinarySignature(name, "${getModifierString(access)} field $name $desc", isStatic(access) ) } } +
                    methods.filter { it.isEffectivelyPublic(classVisibility) }
                            .sortedWith(compareBy({ it.name }, { it.desc }))
                            .map { with(it) { MemberBinarySignature(name, "${getModifierString(access)} fun $name $desc", isStatic(access) ) } }

            ClassBinarySignature(name, superName, outerClassName, modifiers, supertypes, memberSignatures, isPublic)
        }}



fun List<ClassBinarySignature>.filterOutNonPublic(): List<ClassBinarySignature> {
    val classByName = associateBy { it.name }

    fun ClassBinarySignature.isPublicAndAccessible(): Boolean =
            isPublic && (outerName == null || classByName[outerName]?.isPublicAndAccessible() ?: true)

    fun supertypes(superName: String) = generateSequence({ classByName[superName] }, { classByName[it.superName] })

    fun ClassBinarySignature.flattenNonPublicBases(): ClassBinarySignature {

        val nonPublicSupertypes = supertypes(superName).takeWhile { !it.isPublicAndAccessible() }.toList()
        if (nonPublicSupertypes.isEmpty())
            return this

        val inheritedStaticSignatures = nonPublicSupertypes.flatMap { it.memberSignatures.filter { it.isStatic }}

        // not covered the case when there is public superclass after chain of private superclasses
        return this.copy(memberSignatures = memberSignatures + inheritedStaticSignatures, supertypes = supertypes - superName)
    }

    return filter { it -> it.isPublicAndAccessible() }.map { it.flattenNonPublicBases() }
}

fun List<ClassBinarySignature>.dump() = dump(to = System.out)

fun <T: Appendable> List<ClassBinarySignature>.dump(to: T): T = to.apply { this@dump.forEach {
    appendln(it.signature)
    it.memberSignatures.forEach { appendln(it.signature) }
    appendln("------------------\n")
}}

