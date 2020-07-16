package org.jetbrains.dokka.utilities

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import java.io.File
import java.lang.reflect.Type
import com.fasterxml.jackson.core.type.TypeReference as JacksonTypeReference

private val objectMapper = run {
    val module = SimpleModule().apply {
        addSerializer(FileSerializer)
    }
    jacksonObjectMapper()
        .registerModule(module)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

@PublishedApi
internal class TypeReference<T> private constructor(
    internal val jackson: JacksonTypeReference<T>
) {
    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        internal inline operator fun <reified T> invoke(): TypeReference<T> = TypeReference(jacksonTypeRef())
    }
}

@PublishedApi
internal fun toJsonString(value: Any): String = objectMapper.writeValueAsString(value)

@PublishedApi
internal inline fun <reified T : Any> parseJson(json: String): T {
    return parseJson(json, TypeReference())
}

@PublishedApi
internal fun <T : Any> parseJson(json: String, typeReference: TypeReference<T>): T {
    return objectMapper.readValue(json, typeReference.jackson)
}

private object FileSerializer : StdScalarSerializer<File>(File::class.java) {
    override fun serialize(value: File, g: JsonGenerator, provider: SerializerProvider) {
        g.writeString(value.path)
    }

    override fun getSchema(provider: SerializerProvider, typeHint: Type): JsonNode {
        return createSchemaNode("string", true)
    }

    @Throws(JsonMappingException::class)
    override fun acceptJsonFormatVisitor(visitor: JsonFormatVisitorWrapper, typeHint: JavaType) {
        visitStringFormat(visitor, typeHint)
    }
}
