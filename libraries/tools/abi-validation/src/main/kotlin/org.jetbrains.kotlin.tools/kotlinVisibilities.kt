package org.jetbrains.kotlin.tools

import com.google.gson.internal.Streams
import com.google.gson.stream.JsonReader
import java.io.File

data class ClassVisibility(val name: String, val visibility: String?, val members: Map<MemberSignature, MemberVisibility>)
data class MemberVisibility(val member: MemberSignature, val declaration: String?, val visibility: String?)
data class MemberSignature(val name: String, val desc: String)

private fun isPublic(visibility: String?, isPublishedApi: Boolean) = visibility == null || visibility == "public" || visibility == "protected"  || (isPublishedApi && visibility == "internal")
fun ClassVisibility.isPublic(isPublishedApi: Boolean) = isPublic(visibility, isPublishedApi)
fun MemberVisibility.isPublic(isPublishedApi: Boolean) = isPublic(visibility, isPublishedApi)

fun MemberVisibility.isLateInit() = declaration != null && "lateinit var " in declaration

private val varValPrefix = Regex("va[lr]\\s+")
fun ClassVisibility.findSetterForProperty(property: MemberVisibility): MemberVisibility? {
    // ad-hoc solution:
    val declaration = property.declaration ?: return null
    val match = varValPrefix.find(declaration) ?: return null
    val name = declaration.substring(match.range.endInclusive + 1).substringBefore(':')
    val setterName = "<set-$name>"
    return members.values.find { it.declaration?.contains(setterName) ?: false }
}

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
                val members = getAsJsonArray("members").map { it ->
                    with(it.asJsonObject) {
                        val name = getAsJsonPrimitive("name").asString
                        val desc = getAsJsonPrimitive("desc").asString
                        val declaration = getAsJsonPrimitive("declaration")?.asString
                        val visibility = getAsJsonPrimitive("visibility")?.asString
                        MemberVisibility(MemberSignature(name, desc), declaration, visibility)
                    }
                }
                ClassVisibility(name, visibility, members.associateByTo(hashMapOf()) { it.member })
            }
        }
        jsonReader.endArray()
    }

    return result.associateByTo(hashMapOf()) { it.name }
}


