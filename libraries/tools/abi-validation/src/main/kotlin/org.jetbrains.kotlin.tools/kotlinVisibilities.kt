package org.jetbrains.kotlin.tools

import com.google.gson.internal.Streams
import com.google.gson.stream.JsonReader
import java.io.File

data class ClassVisibility(val name: String, val visibility: String?, val members: Map<MemberSignature, MemberVisibility>)
data class MemberVisibility(val member: MemberSignature, val visibility: String?)
data class MemberSignature(val name: String, val desc: String)

private fun isPublic(visibility: String?) = visibility == null || visibility == "public" || visibility == "protected"
fun ClassVisibility.isPublic() = isPublic(visibility)
fun MemberVisibility.isPublic() = isPublic(visibility)


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
                        val visibility = getAsJsonPrimitive("visibility")?.asString
                        MemberVisibility(MemberSignature(name, desc), visibility)
                    }
                }
                ClassVisibility(name, visibility, members.associateByTo(hashMapOf()) { it.member })
            }
        }
        jsonReader.endArray()
    }

    return result.associateByTo(hashMapOf()) { it.name }
}


