package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionIn
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionOut
import org.jetbrains.kotlin.fir.types.ConeNullability
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.isNullable
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.withNullability
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlinx.dataframe.plugin.PluginDataFrameSchema
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance

val DF_CLASS_ID: ClassId
    get() = ClassId.topLevel(FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe", "DataFrame")))

val COLUM_GROUP_CLASS_ID: ClassId
    get() = ClassId(FqName("org.jetbrains.kotlinx.dataframe.columns"), Name.identifier("ColumnGroup"))

val DATA_ROW_CLASS_ID: ClassId
    get() = ClassId(FqName.fromSegments(listOf("org", "jetbrains", "kotlinx", "dataframe")), Name.identifier("DataRow"))

interface KotlinTypeFacade {
    val session: FirSession
    val resolutionPath: String? get() = null
    val cache: FirCache<String, PluginDataFrameSchema, KotlinTypeFacade>
    val schemasDirectory: String?

    fun Marker.type() = type

    val anyDataFrame get() = ConeClassLikeTypeImpl(
        ConeClassLikeLookupTagImpl(DF_CLASS_ID),
        typeArguments = arrayOf(session.builtinTypes.anyType.type),
        isNullable = false
    ).wrap()

    val anyRow get() = ConeClassLikeTypeImpl(
        ConeClassLikeLookupTagImpl(DATA_ROW_CLASS_ID),
        typeArguments = arrayOf(session.builtinTypes.anyType.type),
        isNullable = false
    ).wrap()

    fun Marker.toColumnGroup() = ConeClassLikeTypeImpl(
        ConeClassLikeLookupTagImpl(COLUM_GROUP_CLASS_ID),
        typeArguments = arrayOf(type.typeArguments[0]),
        isNullable = false
    ).wrap()

    fun from(type: KType): Marker {
        return Marker(fromImpl(type))
    }

    private fun fromImpl(type: KType): ConeClassLikeType {
        val classId = type.classId()
        val coneType = classId.constructClassLikeType(
            typeArguments = type.arguments.mapToConeTypeProjection(),
            isNullable = type.isMarkedNullable
        )
        return coneType
    }

    fun KType.classId(): ClassId {
        if (classifier == Void::class) return session.builtinTypes.nullableNothingType.id
        val classifier = classifier ?: error("")
        val klass = classifier as? KClass<*> ?: error("")
        val fqName = klass.qualifiedName ?: error("")
        return ClassId(
            FqName(fqName.substringBeforeLast(".", missingDelimiterValue = "")),
            Name.identifier(fqName.substringAfterLast("."))
        )
    }

    private fun List<KTypeProjection>.mapToConeTypeProjection(): Array<out ConeTypeProjection> {
        return Array(size) {
            val typeProjection = get(it)
            val type = typeProjection.type
            val variance = typeProjection.variance
            if (type != null && variance != null) {
                val coneType = fromImpl(type)
                when (variance) {
                    KVariance.INVARIANT -> coneType
                    KVariance.IN -> ConeKotlinTypeProjectionIn(coneType)
                    KVariance.OUT -> ConeKotlinTypeProjectionOut(coneType)
                }
            } else {
                ConeStarProjection
            }
        }
    }

    fun Marker.changeNullability(map: (Boolean) -> Boolean): Marker {
        val coneNullability = when (map(type.isNullable)) {
            true -> ConeNullability.NULLABLE
            false -> ConeNullability.NOT_NULL
        }
        return Marker(type = type.withNullability(coneNullability, session.typeContext))
    }

    fun Marker.isList(): Boolean {
        return type.isBuiltinType(List, isNullable = null)
    }

    fun Marker.typeArgument(): Marker {
        val argument = when (val argument = type.typeArguments[0]) {
            is ConeKotlinType -> argument
            else -> error("${argument::class} ${argument}")
        }
        return Marker(argument)
    }
}

private val List = "List".collectionsId()

private fun ConeKotlinType.isBuiltinType(classId: ClassId, isNullable: Boolean?): Boolean {
    if (this !is ConeClassLikeType) return false
    return lookupTag.classId == classId && (isNullable == null || type.isNullable == isNullable)
}

private fun String.collectionsId() = ClassId(StandardClassIds.BASE_COLLECTIONS_PACKAGE, Name.identifier(this))

class KotlinTypeFacadeImpl(
    override val session: FirSession,
    override val cache: FirCache<String, PluginDataFrameSchema, KotlinTypeFacade>,
    override val schemasDirectory: String?

) : KotlinTypeFacade

class Marker private constructor(internal val type: ConeKotlinType) {
    companion object {
        operator fun invoke(type: ConeKotlinType): Marker {
            val type = if (type is ConeFlexibleType) {
                type.lowerBound
            } else {
                type
            }
            return Marker(type)
        }
    }

    override fun toString(): String {
        return "Marker(type=$type (${type::class}))"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Marker

        return type == other.type
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }
}

fun ConeKotlinType.wrap(): Marker = Marker(this)



