package org.jetbrains.kotlinx.dataframe.plugin.impl.data

import kotlinx.serialization.Serializable
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleCol
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleFrameColumn
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionIn
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionOut
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.dataframe.plugin.extensions.KotlinTypeFacade
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.SerializableColumn.Column
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.SerializableColumn.ColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.impl.data.SerializableColumn.FrameColumn
import org.jetbrains.kotlinx.dataframe.schema.ColumnSchema
import org.jetbrains.kotlinx.dataframe.schema.DataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.extensions.wrap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance

val KotlinTypeFacade.toPluginDataFrameSchema: DataFrameSchema.() -> PluginDataFrameSchema
    get() =  {
    PluginDataFrameSchema(
        columns = columns.map { (name, columnSchema) ->
            when (columnSchema) {
                is ColumnSchema.Value -> {
                    val type = from(columnSchema.type)
                    SimpleCol(name, type)
                    // class Data(val i: Int) // local class
                    // val df = dataFrameOf("col")(Data(1))
                    // val schema = df.schema()
                    // generateInterface(schema)
                }
                is ColumnSchema.Group -> SimpleColumnGroup(name, columnSchema.schema.toPluginDataFrameSchema().columns(), anyRow)
                is ColumnSchema.Frame -> SimpleFrameColumn(
                    name,
                    columnSchema.schema.toPluginDataFrameSchema().columns(),
                    anyDataFrame
                )
                else -> TODO()
            }
        }
    )
}



val KotlinTypeFacade.deserializeToPluginDataFrameSchema: SerializableSchema.() -> PluginDataFrameSchema
    get() =  {
    PluginDataFrameSchema(
        columns = columns.map {
            when (it) {
                is Column -> {
                    val type = type(it.kType)

                    SimpleCol(it.name, type.wrap())
                    // class Data(val i: Int) // local class
                    // val df = dataFrameOf("col")(Data(1))
                    // val schema = df.schema()
                    // generateInterface(schema)
                }
                is ColumnGroup -> SimpleColumnGroup(it.name, SerializableSchema(it.columns).deserializeToPluginDataFrameSchema().columns(), anyRow)
                is FrameColumn -> SimpleFrameColumn(
                    it.name,
                    SerializableSchema(it.columns).deserializeToPluginDataFrameSchema().columns(),
                    anyDataFrame
                )
                else -> TODO()
            }
        }
    )
}

private fun type(kType: SerializableKType): ConeClassLikeType {
    val id = ClassId(
        FqName(kType.qualifiedName.substringBeforeLast(".", missingDelimiterValue = "")),
        Name.identifier(kType.qualifiedName.substringAfterLast("."))
    )
    val type = id.constructClassLikeType(
        typeArguments = kType.arguments.mapToConeTypeProjection(),
        isNullable = kType.isMarkedNullable
    )
    return type
}


@Serializable
class IoSchema(val argument: String, val schema: SerializableSchema)

enum class SerializableVariance {
    INVARIANT, IN, OUT
}

@Serializable
data class SerializableArgument(val kType: SerializableKType, val variance: SerializableVariance): TypeProjection

@Serializable
data object StarProjection : TypeProjection

@Serializable
sealed interface TypeProjection

@Serializable
class SerializableKType(val qualifiedName: String, val isMarkedNullable: Boolean, val arguments: List<TypeProjection>)

@Serializable
sealed interface SerializableColumn {
    @Serializable class Column(val name: String, val kType: SerializableKType): SerializableColumn
    @Serializable class ColumnGroup(val name: String, val columns: List<SerializableColumn>): SerializableColumn
    @Serializable class FrameColumn(val name: String, val columns: List<SerializableColumn>): SerializableColumn
}

@Serializable
class SerializableSchema(val columns: List<SerializableColumn>)


fun DataFrameSchema.serialize(): SerializableSchema {
    return SerializableSchema(
        columns = columns.map { (name, columnSchema) ->
            when (columnSchema) {
                is ColumnSchema.Value -> {
                    val serializableKType = from(columnSchema.type)
                    Column(name, serializableKType)
                    // class Data(val i: Int) // local class
                    // val df = dataFrameOf("col")(Data(1))
                    // val schema = df.schema()
                    // generateInterface(schema)
                }
                is ColumnSchema.Group -> ColumnGroup(name, columnSchema.schema.serialize().columns)
                is ColumnSchema.Frame -> FrameColumn(name, columnSchema.schema.serialize().columns)
                else -> TODO()
            }
        }
    )
}

private fun from(type: KType): SerializableKType {
    val qualifiedName = type.from()
    val serializableKType = SerializableKType(
        qualifiedName,
        type.isMarkedNullable,
        type.arguments.mapToConeTypeProjection()
    )
    return serializableKType
}


private fun List<KTypeProjection>.mapToConeTypeProjection(): List<TypeProjection> = List(size) {
    val typeProjection = get(it)
    val type = typeProjection.type
    val variance = typeProjection.variance
    if (type != null && variance != null) {
        val coneType = from(type)
        val variance = when (variance) {
            KVariance.INVARIANT -> SerializableVariance.INVARIANT
            KVariance.IN -> SerializableVariance.IN
            KVariance.OUT -> SerializableVariance.OUT
        }
        SerializableArgument(coneType, variance)
    } else {
        StarProjection
    }
}

private fun List<TypeProjection>.mapToConeTypeProjection(): Array<out ConeTypeProjection> {
    return Array(size) {
        when (val typeProjection = get(it)) {
            StarProjection -> ConeStarProjection
            is SerializableArgument -> {
                val coneType = type(typeProjection.kType)
                when (typeProjection.variance) {
                    SerializableVariance.INVARIANT -> coneType
                    SerializableVariance.IN -> ConeKotlinTypeProjectionIn(coneType)
                    SerializableVariance.OUT -> ConeKotlinTypeProjectionOut(coneType)
                }
            }
        }
    }
}


fun KType.from(): String {
    val classifier = classifier ?: error("")
    val klass = classifier as? KClass<*> ?: error("")
    val fqName = klass.qualifiedName ?: error("")
    return fqName
}
