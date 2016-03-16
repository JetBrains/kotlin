package org.jetbrains.kotlin.tools

import com.google.gson.internal.Streams
import com.google.gson.stream.JsonReader
import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import java.io.File
import java.util.jar.JarFile
import kotlin.comparisons.compareBy

fun main(args: Array<String>) {
    var src = args[0]
    println(src)
    println("------------------\n");
    val visibilities = readKotlinVisibilities(File("""stdlib/target/stdlib-declarations.json"""))
    getBinaryAPI(JarFile(src), visibilities).filterOutNonPublic().dump()
}



val ACCESS_NAMES = mapOf(
        Opcodes.ACC_PUBLIC to "public",
        Opcodes.ACC_PROTECTED to "protected",
        Opcodes.ACC_PRIVATE to "private",
        Opcodes.ACC_STATIC to "static",
        Opcodes.ACC_FINAL to "final",
        Opcodes.ACC_ABSTRACT  to "abstract",
        Opcodes.ACC_SYNTHETIC to "synthetic",
        Opcodes.ACC_INTERFACE to "interface",
        Opcodes.ACC_ANNOTATION to "annotation")

fun JarFile.classEntries() = entries().asSequence().filter {
    !it.isDirectory && it.name.endsWith(".class")
}

data class ClassBinarySignature(val name: String, val outerName: String?, val signature: String, val memberSignatures: List<String>, val isPublic: Boolean)

fun getBinaryAPI(jar: JarFile, visibilityMap: Map<String, ClassVisibility>): List<ClassBinarySignature> = jar.classEntries()
        .map { entry ->
            jar.getInputStream(entry).use { stream ->
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

            val classSignature = "${getModifierString(access)} class $name" +
                    if (supertypes.isEmpty()) "" else ": ${supertypes.joinToString()}"

            val memberSignatures =
                    fields.filter { it.isPublic() }
                            .sortedBy { it.name }
                            .map { with(it) { "${getModifierString(access)} field $name $desc" } } +
                    methods.filter { it.isEffectivelyPublic(classVisibility) }
                            .sortedWith(compareBy({ it.name }, { it.desc }))
                            .map { with(it) { "${getModifierString(access)} fun $name $desc" } }

            ClassBinarySignature(name, outerClassName, classSignature, memberSignatures, isPublic)
        }}



fun List<ClassBinarySignature>.filterOutNonPublic(): List<ClassBinarySignature> {
    val classByName = associateBy { it.name }

    fun ClassBinarySignature.isPublicAndAccessible(): Boolean =
            isPublic && (outerName == null || classByName[outerName]?.isPublicAndAccessible() ?: true)

    return filter { it.isPublicAndAccessible() }
}

fun List<ClassBinarySignature>.dump() = forEach {
    println(it.signature)
    it.memberSignatures.forEach { println(it) }
    println("------------------\n")
}




fun isPublic(access: Int) = access and Opcodes.ACC_PUBLIC != 0 || access and Opcodes.ACC_PROTECTED != 0
fun getModifiers(access: Int): List<String> = ACCESS_NAMES.entries.mapNotNull { if (access and it.key != 0) it.value else null }
fun getModifierString(access: Int): String = getModifiers(access).joinToString(" ")

fun ClassNode.isSynthetic() = access and Opcodes.ACC_SYNTHETIC != 0
fun MethodNode.isSynthetic() = access and Opcodes.ACC_SYNTHETIC != 0
fun ClassNode.isPublic() = isPublic(access)
fun MethodNode.isPublic() = isPublic(access)
fun FieldNode.isPublic() = isPublic(access)


fun ClassNode.isEffectivelyPublic(classVisibility: ClassVisibility?) =
        isPublic()
        && !isLocal()
        && !isWhenMappings()
        && (classVisibility?.isPublic() ?: true)
        && !isNonPublicFileOrFacade(classVisibility)

fun ClassNode.isNonPublicFileOrFacade(classVisibility: ClassVisibility?) =
        isFileOrMultipartFacade()
        && methods.none { it.isEffectivelyPublic(classVisibility) }
        && fields.none { it.isPublic() }


fun MethodNode.isEffectivelyPublic(classVisibility: ClassVisibility?) =
        isPublic()
        && (classVisibility?.members?.get(MemberSignature(name, desc))?.isPublic() ?: true)
        && !isAccessMethod()



fun ClassNode.isLocal() = innerClasses.filter { it.name == name && it.innerName == null && it.outerName == null }.count() == 1
fun ClassNode.isWhenMappings() = isSynthetic() && name.endsWith("\$WhenMappings")
fun MethodNode.isAccessMethod() = isSynthetic() && name.startsWith("access\$")



private val FILE_OR_MULTIPART_FACADE_KINDS = listOf(2, 4)
fun ClassNode.isFileOrMultipartFacade() = kotlinClassKind.let { it != null && it in FILE_OR_MULTIPART_FACADE_KINDS }


val ClassNode.kotlinClassKind: Int?
    get() = visibleAnnotations
            ?.filter { it.desc == "Lkotlin/Metadata;" }
            ?.map { (it.values.annotationValue("k") as? Int) }
            ?.firstOrNull()

private fun List<Any>.annotationValue(key: String): Any? {
    for (index in (0 .. size / 2 - 1)) {
        if (this[index*2] == key)
            return this[index*2 + 1]
    }
    return null
}

val ClassNode.outerClassName: String?
    get() = innerClasses.singleOrNull { it.name == name }?.outerName



fun readKotlinVisibilities(declarationFile: File): Map<String, ClassVisibility> {
    val result = mutableListOf<ClassVisibility>()
    declarationFile.bufferedReader().use { reader ->
        val jsonReader = JsonReader(reader)
        jsonReader.beginArray()
        while (jsonReader.hasNext()) {
            val classObject = Streams.parse(jsonReader).asJsonObject
            result += with (classObject) {
                val name = getAsJsonPrimitive("class").asString
                val visibility = getAsJsonPrimitive("visibility")?.asString
                val members = getAsJsonArray("members").map { it -> with(it.asJsonObject) {
                    val name = getAsJsonPrimitive("name").asString
                    val desc = getAsJsonPrimitive("desc").asString
                    val visibility = getAsJsonPrimitive("visibility")?.asString
                    MemberVisibility(MemberSignature(name, desc), visibility)
                }}
                ClassVisibility(name, visibility, members.associateByTo(hashMapOf()) { it.member })
            }
        }
        jsonReader.endArray()
    }

    return result.associateByTo(hashMapOf()) { it.name }
}


data class ClassVisibility(val name: String, val visibility: String?, val members: Map<MemberSignature, MemberVisibility>)

fun ClassVisibility.isPublic() = visibility == "public"

data class MemberVisibility(val member: MemberSignature, val visibility: String?)
data class MemberSignature(val name: String, val desc: String)

fun MemberVisibility.isPublic() = visibility == "public" || visibility == "protected"
