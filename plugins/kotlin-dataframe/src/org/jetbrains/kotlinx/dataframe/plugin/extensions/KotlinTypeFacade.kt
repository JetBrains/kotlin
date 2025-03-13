package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.fir.types.withNullability
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

interface KotlinTypeFacade : SessionContext {
    val isTest: Boolean

    fun Marker.type() = type

    fun Marker.changeNullability(map: (Boolean) -> Boolean): Marker {
        return Marker(type = type.withNullability(map(type.isMarkedNullable), session.typeContext))
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

interface SessionContext {
    val session: FirSession
}

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
    override val isTest: Boolean
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



