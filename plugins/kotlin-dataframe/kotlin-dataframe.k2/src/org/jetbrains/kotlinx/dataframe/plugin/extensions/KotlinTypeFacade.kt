package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

interface KotlinTypeFacade : SessionHolder {
    val isTest: Boolean
}

context(sessionHolder: SessionHolder)
fun ColumnType.changeNullability(map: (Boolean) -> Boolean): ColumnType {
    return ColumnType(type = coneType.withNullability(map(coneType.isMarkedNullable), sessionHolder.session.typeContext))
}

fun ColumnType.isList(): Boolean {
    return coneType.isBuiltinType(List, isNullable = null)
}

context(sessionHolder: SessionHolder)
fun ColumnType.typeArgument(): ColumnType {
    val argument = when (val argument = coneType.typeArguments[0]) {
        is ConeKotlinType -> argument
        else -> error("${argument::class} $argument")
    }
    return ColumnType(argument)
}

fun SessionContext(session: FirSession) = object : SessionHolder {
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

class ColumnType private constructor(internal val coneType: ConeKotlinType) {
    companion object {
        context(context: SessionHolder)
        operator fun invoke(type: ConeKotlinType): ColumnType {
            val type = if (type is ConeFlexibleType) {
                type.lowerBound
            } else {
                type
            }
            return ColumnType(type)
        }

        fun convertUnsafe(type: ConeKotlinType) = ColumnType(type)
    }

    override fun toString(): String {
        return "ColumnType(coneType=$coneType))"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ColumnType

        return coneType == other.coneType
    }

    override fun hashCode(): Int {
        return coneType.hashCode()
    }
}

context(context: SessionHolder)
fun ConeKotlinType.wrap(): ColumnType = ColumnType(this)

// The resulting type should not be materialized as a type of a property. Only for testing
fun ConeKotlinType.wrapUnsafe(): ColumnType = ColumnType.convertUnsafe(type = this)



