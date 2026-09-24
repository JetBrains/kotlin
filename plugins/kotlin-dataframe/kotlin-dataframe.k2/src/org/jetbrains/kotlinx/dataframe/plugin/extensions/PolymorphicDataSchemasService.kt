/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe.plugin.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.isSubtypeOf
import org.jetbrains.kotlin.fir.withSession
import org.jetbrains.kotlinx.dataframe.plugin.impl.PluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleColumnGroup
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleDataColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.SimpleFrameColumn
import org.jetbrains.kotlinx.dataframe.plugin.impl.api.flatten
import org.jetbrains.kotlinx.dataframe.plugin.pluginDataFrameSchema
import org.jetbrains.kotlinx.dataframe.plugin.utils.Names

/**
 * Proof of concept for [KDF#2097](https://github.com/Kotlin/dataframe/issues/2097): polymorphism for the compiler plugin.
 *
 * Collects `@DataSchema` interfaces declared in the module being compiled and matches them against schemas inferred by
 * the plugin. [FunctionCallTransformer] uses the result to add every compatible `@DataSchema` interface as a supertype
 * of the local marker class it generates for a refined call, which makes declarations like
 *
 * ```kotlin
 * @DataSchema
 * interface UserLike {
 *     val name: String
 *     val age: Int
 * }
 *
 * fun DataFrame<UserLike>.nameAndAge(): String = rows().joinToString { "$name is $age years old" }
 * ```
 *
 * applicable to any dataframe whose schema happens to be compatible with `UserLike`, without an explicit `cast`.
 * `DataFrame` and `DataRow` are covariant in their schema parameter, so the injected supertype is enough to make the
 * extension resolve.
 *
 * Only the final marker gets the supertypes, not the intermediate schema classes, and only interfaces of the current
 * module are considered. Both limitations are intentional and keep the amount of generated supertypes bounded.
 */
class PolymorphicDataSchemasService(session: FirSession) : FirExtensionSessionComponent(session) {
    companion object {
        private val PREDICATE = LookupPredicate.create {
            annotated(Names.DATA_SCHEMA_CLASS_ID.asSingleFqName())
        }
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }

    private class Candidate(val type: ConeClassLikeType, val schema: PluginDataFrameSchema)

    /**
     * `@DataSchema` declarations that can act as a supertype of a generated marker:
     * - interfaces only, a marker is a class and can neither extend a `data class` nor have two class supertypes
     * - no type parameters, there is nothing to infer type arguments from
     *     - TODO: maybe we can add support for this by automatically filling in type arguments.
     *         No reason why this shouldn't work, right?
     *     ```kt
     *     interface Name<T> { val name: T }
     *
     *     fun Name<String>.printName() = println(name)
     *
     *     dataFrameOf("name")("a").printName()
     *     ```
     * - a non-empty schema, every dataframe is trivially compatible with an empty one
     */
    private val candidates: List<Candidate> by lazy {
        withSession(session) {
            session.predicateBasedProvider.getSymbolsByPredicate(PREDICATE)
                .filterIsInstance<FirRegularClassSymbol>()
                .filter {
                    it.isInterface &&
                            !it.isLocal &&
                            // TODO?
                            it.typeParameterSymbols.isEmpty()
                    // TODO should we filter on visibility?
                }
                .mapNotNull { symbol ->
                    val type = symbol.defaultType()
                    val schema = type.pluginDataFrameSchema()
                    if (schema.columns().isEmpty()) null else Candidate(type, schema)
                }
        }
    }

    /**
     * `@DataSchema` interfaces of the module that [schema] is compatible with.
     *
     * An interface implied by another returned interface is dropped: for `interface Named` and
     * `interface UserLike : Named` only `UserLike` is returned, `Named` already comes with it.
     */
    fun compatibleDataSchemas(schema: PluginDataFrameSchema): List<ConeClassLikeType> {
        if (schema.columns().isEmpty()) return emptyList()
        val matched = candidates.filter { schema.satisfies(it.schema) }
        return matched
            .filter { candidate ->
                matched.none { other ->
                    other !== candidate &&
                            other.type.isSubtypeOf(candidate.type, session)
                }
            }
            .map { it.type }
    }

    /**
     * Whether every column of [target] is present in this schema, at the same path, with the same column kind and a
     * type that can be used where the type declared by [target] is expected. Extra columns are allowed, that's the
     * whole point: a dataframe with a `favoriteColor` column on top of `name` and `age` still is `UserLike`.
     */
    private fun PluginDataFrameSchema.satisfies(target: PluginDataFrameSchema): Boolean {
        val actualColumns = flatten(includeFrames = true).associate { it.path.path to it.column }
        return target.flatten(includeFrames = true).all { expected ->
            val actual = actualColumns[expected.path.path] ?: return@all false
            when (val expectedColumn = expected.column) {
                is SimpleDataColumn ->
                    actual is SimpleDataColumn &&
                            actual.type.coneType.isSubtypeOf(expectedColumn.type.coneType, session)
                is SimpleColumnGroup -> actual is SimpleColumnGroup
                is SimpleFrameColumn -> actual is SimpleFrameColumn
            }
        }
    }
}

val FirSession.polymorphicDataSchemasService: PolymorphicDataSchemasService by FirSession.sessionComponentAccessor()
