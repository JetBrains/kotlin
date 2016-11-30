package org.jetbrains.kotlin.tools

import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import java.io.File
import java.io.InputStream
import java.util.jar.JarFile

fun main(args: Array<String>) {
    var src = args[0]
    println(src)
    println("------------------\n");
    val visibilities = readKotlinVisibilities(File("""stdlib/target/stdlib-declarations.json"""))
    getBinaryAPI(JarFile(src), visibilities).filterOutNonPublic().dump()
}


fun JarFile.classEntries() = entries().asSequence().filter { !it.isDirectory && it.name.endsWith(".class") }


fun getBinaryAPI(jar: JarFile, visibilityMap: Map<String, ClassVisibility>): List<ClassBinarySignature> =
        getBinaryAPI(jar.classEntries().map { entry -> jar.getInputStream(entry) }, visibilityMap)

fun getBinaryAPI(classStreams: Sequence<InputStream>, visibilityMap: Map<String, ClassVisibility>): List<ClassBinarySignature> =
        classStreams.map { it.use { stream ->
                val classNode = ClassNode()
                ClassReader(stream).accept(classNode, ClassReader.SKIP_CODE)
                classNode
            }
        }
        .map { with(it) {
            val classVisibility = visibilityMap[name]
            val classAccess = AccessFlags(effectiveAccess and Opcodes.ACC_STATIC.inv())

            val supertypes = listOf(superName) - "java/lang/Object" + interfaces.sorted()

            val memberSignatures = (
                    fields.map { with(it) { FieldBinarySignature(name, desc, isPublishedApi(), AccessFlags(access)) } } +
                    methods.map { with(it) { MethodBinarySignature(name, desc, isPublishedApi(), AccessFlags(access)) } }
            ).filter {
                it.isEffectivelyPublic(classAccess, classVisibility)
            }

            ClassBinarySignature(name, superName, outerClassName, supertypes, memberSignatures, classAccess, isEffectivelyPublic(classVisibility), isFileOrMultipartFacade() || isDefaultImpls())
        }}
        .asIterable()
        .sortedBy { it.name }



fun List<ClassBinarySignature>.filterOutNonPublic(): List<ClassBinarySignature> {
    val classByName = associateBy { it.name }

    fun ClassBinarySignature.isPublicAndAccessible(): Boolean =
            isEffectivelyPublic &&
                    (outerName == null || classByName[outerName]?.let { outerClass ->
                        !(this.access.isProtected && outerClass.access.isFinal)
                                && outerClass.isPublicAndAccessible()
                    } ?: true)

    fun supertypes(superName: String) = generateSequence({ classByName[superName] }, { classByName[it.superName] })

    fun ClassBinarySignature.flattenNonPublicBases(): ClassBinarySignature {

        val nonPublicSupertypes = supertypes(superName).takeWhile { !it.isPublicAndAccessible() }.toList()
        if (nonPublicSupertypes.isEmpty())
            return this

        val inheritedStaticSignatures = nonPublicSupertypes.flatMap { it.memberSignatures.filter { it.access.isStatic }}

        // not covered the case when there is public superclass after chain of private superclasses
        return this.copy(memberSignatures = memberSignatures + inheritedStaticSignatures, supertypes = supertypes - superName)
    }

    return filter { it.isPublicAndAccessible() }
            .map { it.flattenNonPublicBases() }
            .filterNot { it.isNotUsedWhenEmpty && it.memberSignatures.isEmpty()}
}

fun List<ClassBinarySignature>.dump() = dump(to = System.out)

fun <T: Appendable> List<ClassBinarySignature>.dump(to: T): T = to.apply { this@dump.forEach {
    append(it.signature).appendln(" {")
    it.memberSignatures.sortedWith(MEMBER_SORT_ORDER).forEach { append("\t").appendln(it.signature) }
    appendln("}\n")
}}

