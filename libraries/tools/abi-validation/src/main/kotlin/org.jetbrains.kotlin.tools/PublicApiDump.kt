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


data class ClassBinarySignature(val name: String, val outerName: String?, val modifiers: String, val supertypes: List<String>, val memberSignatures: List<String>, val isPublic: Boolean)

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

            val modifiers = getModifierString(access)

            val memberSignatures =
                    fields.filter { it.isPublic() }
                            .sortedBy { it.name }
                            .map { with(it) { "${getModifierString(access)} field $name $desc" } } +
                    methods.filter { it.isEffectivelyPublic(classVisibility) }
                            .sortedWith(compareBy({ it.name }, { it.desc }))
                            .map { with(it) { "${getModifierString(access)} fun $name $desc" } }

            ClassBinarySignature(name, outerClassName, modifiers, supertypes, memberSignatures, isPublic)
        }}



fun List<ClassBinarySignature>.filterOutNonPublic(): List<ClassBinarySignature> {
    val classByName = associateBy { it.name }

    fun ClassBinarySignature.isPublicAndAccessible(): Boolean =
            isPublic && (outerName == null || classByName[outerName]?.isPublicAndAccessible() ?: true)

    return filter { it -> it.isPublicAndAccessible() }
}

fun List<ClassBinarySignature>.dump() = dump(to = System.out)

fun <T: Appendable> List<ClassBinarySignature>.dump(to: T): T = to.apply { this@dump.forEach {
    appendln(it.signature)
    it.memberSignatures.forEach { appendln(it) }
    appendln("------------------\n")
}}

