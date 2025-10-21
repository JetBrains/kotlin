package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

interface KotlinTypeFacade : SessionContext {
    val isTest: Boolean

    fun Marker.changeNullability(map: (Boolean) -> Boolean): Marker {
        return Marker(type = coneType.withNullability(map(coneType.isMarkedNullable), session.typeContext))
    }

    fun Marker.isList(): Boolean {
        return coneType.isBuiltinType(List, isNullable = null)
    }

    fun Marker.typeArgument(): Marker {
        val argument = when (val argument = coneType.typeArguments[0]) {
            is ConeKotlinType -> argument
            else -> error("${argument::class} $argument")
        }
        return Marker(argument)
    }
}

typealias SessionContext = SessionHolder

fun SessionContext(session: FirSession) = object : SessionContext {
    override val session: FirSession = session
}

private val List = "List".collectionsId()

private fun ConeKotlinType.isBuiltinType(classId: ClassId, isNullable: Boolean?): Boolean {
    if (this !is ConeClassLikeType) return false
    return lookupTag.classId == classId && (isNullable == null || isMarkedNullable == isNullable)
}

private fun String.collectionsId() = ClassId(StandardClassIds.BASE_COLLECTIONS_PACKAGE, Name.identifier(this))

class KotlinTypeFacadeImpl(
    override val session: FirSession,
    override val isTest: Boolean,
) : KotlinTypeFacade

class Marker private constructor(internal val coneType: ConeKotlinType) {
    companion object {
        context(context: SessionContext)
        operator fun invoke(type: ConeKotlinType): Marker {
            val type = if (type is ConeFlexibleType) {
                type.lowerBound
            } else {
                type
            }
            return Marker(type)
        }

        fun convertUnsafe(type: ConeKotlinType) = Marker(type)
    }

    override fun toString(): String {
        return "Marker(type=$coneType (${coneType::class}))"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Marker

        return coneType == other.coneType
    }

    override fun hashCode(): Int {
        return coneType.hashCode()
    }
}

context(context: SessionContext)
fun ConeKotlinType.wrap(): Marker = Marker(this)

// The resulting type should not be materialized as a type of a property. Only for testing
fun ConeKotlinType.wrapUnsafe(): Marker = Marker.convertUnsafe(type = this)



