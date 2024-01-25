package org.jetbrains.kotlinx.dataframe.plugin

import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

public object ColumnsSerializer : KSerializer<List<SimpleCol>> by ListSerializer(PolymorphicSerializer(SimpleCol::class))

public val pluginSerializationModule: SerializersModule = SerializersModule {
    contextual(ColumnsSerializer)
//    polymorphic(SimpleCol::class, SimpleCol.serializer()) {
//        subclass(SimpleColumnGroup::class)
//        subclass(SimpleFrameColumn::class)
//    }
}

public val pluginJsonFormat: Json = Json {
    serializersModule = pluginSerializationModule
}
