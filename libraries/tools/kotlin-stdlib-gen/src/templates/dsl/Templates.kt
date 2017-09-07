/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package templates

import kotlin.coroutines.experimental.buildSequence

@DslMarker
annotation class TemplateDsl

enum class Keyword(val value: String) {
    Function("fun"),
    Value("val"),
    Variable("var");
}

typealias MemberBuildAction = MemberBuilder.() -> Unit
typealias MemberBuildActionP<TParam> = MemberBuilder.(TParam) -> Unit

private fun def(signature: String, memberKind: Keyword): MemberBuildAction = {
    this.signature = signature
    this.keyword = memberKind
}

fun fn(defaultSignature: String): MemberBuildAction = def(defaultSignature, Keyword.Function)

fun fn(defaultSignature: String, setup: FamilyPrimitiveMemberDsl.() -> Unit): MemberTemplate =
        FamilyPrimitiveMemberDsl().apply {
            builder(fn(defaultSignature))
            setup()
        }

fun MemberBuildAction.byTwoPrimitives(setup: PairPrimitiveMemberDsl.() -> Unit): MemberTemplate =
        PairPrimitiveMemberDsl().apply {
            builder(this@byTwoPrimitives)
            setup()
        }

fun pval(name: String, setup: FamilyPrimitiveMemberDsl.() -> Unit): MemberTemplate =
        FamilyPrimitiveMemberDsl().apply {
            builder(def(name, Keyword.Value))
            setup()
        }

fun pvar(name: String, setup: FamilyPrimitiveMemberDsl.() -> Unit): MemberTemplate =
        FamilyPrimitiveMemberDsl().apply {
            builder(def(name, Keyword.Variable))
            setup()
        }


interface MemberTemplate {
    /** Specifies which platforms this member template should be generated for */
    fun platforms(vararg platforms: Platform)

    fun instantiate(): Sequence<MemberBuilder>

    /** Registers parameterless member builder function */
    fun builder(b: MemberBuildAction)

}

abstract class GenericMemberDsl<TParam> : MemberTemplate {

    sealed class BuildAction {
        class Generic(val action: MemberBuildAction) : BuildAction() {
            operator fun invoke(builder: MemberBuilder) { action(builder) }
        }
        class Parametrized(val action: MemberBuildActionP<*>) : BuildAction() {
            @Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "UNCHECKED_CAST")
            operator fun <TParam> invoke(builder: MemberBuilder, p: @kotlin.internal.NoInfer TParam) {
                (action as MemberBuildActionP<TParam>).invoke(builder, p)
            }
        }
    }

    val buildActions = mutableListOf<BuildAction>()

    private var targetPlatforms = setOf(*Platform.values())
    override fun platforms(vararg platforms: Platform) {
        targetPlatforms = setOf(*platforms)
    }


    private var filterPredicate: ((Family, TParam) -> Boolean)? = null
    /** Sets the filter predicate that is applied to a produced sequence of variations. */
    fun filter(predicate: (Family, TParam) -> Boolean) {
        this.filterPredicate = predicate
    }

    override fun builder(b: MemberBuildAction) { buildActions += BuildAction.Generic(b) }
    /** Registers member builder function with the parameter(s) of this DSL */
    fun builderWith(b: MemberBuildActionP<TParam>) { buildActions += BuildAction.Parametrized(b) }



    /** Provides the sequence of member variation parameters */
    protected abstract fun parametrize(): Sequence<Pair<Family, TParam>>

    private fun Sequence<Pair<Family, TParam>>.applyFilter() =
            filterPredicate?.let { predicate ->
                filter { (family, p) -> predicate(family, p) }
            } ?: this


    override fun instantiate(): Sequence<MemberBuilder> {
        val specificPlatforms by lazy { targetPlatforms - Platform.Common }

        fun platformMemberBuilders(family: Family, p: TParam) =
                if (Platform.Common in targetPlatforms) {
                    val commonMemberBuilder = createMemberBuilder(Platform.Common, family, p)
                    mutableListOf(commonMemberBuilder).also { builders ->
                        if (commonMemberBuilder.hasPlatformSpecializations) {
                            specificPlatforms.mapTo(builders) {
                                createMemberBuilder(it, family, p)
                            }
                        }
                    }
                } else {
                    targetPlatforms.map { createMemberBuilder(it, family, p) }
                }

        return parametrize()
                .applyFilter()
                .map { (family, p) -> platformMemberBuilders(family, p) }
                .flatten()
    }

    private fun createMemberBuilder(platform: Platform, family: Family, p: TParam): MemberBuilder {
        return MemberBuilder(targetPlatforms, platform, family).also { builder ->
            for (action in buildActions) {
                when (action) {
                    is BuildAction.Generic -> action(builder)
                    is BuildAction.Parametrized -> action<TParam>(builder, p)
                }
            }
        }
    }

}

private fun defaultPrimitives(f: Family): Set<PrimitiveType> =
        if (f.isPrimitiveSpecialization) PrimitiveType.defaultPrimitives else emptySet()

@TemplateDsl
class FamilyPrimitiveMemberDsl : GenericMemberDsl<PrimitiveType?>() {

    private val familyPrimitives = mutableMapOf<Family, Set<PrimitiveType>>()

    fun include(vararg fs: Family) {
        for (f in fs) familyPrimitives[f] = defaultPrimitives(f)
    }

    fun include(fs: Collection<Family>) {
        for (f in fs) familyPrimitives[f] = defaultPrimitives(f)
    }

    fun includeDefault() {
        include(Family.defaultFamilies)
    }

    fun include(f: Family, primitives: Set<PrimitiveType>) {
        familyPrimitives[f] = primitives
    }

    fun exclude(vararg ps: PrimitiveType) {
        val toExclude = ps.toSet()
        for (e in familyPrimitives) {
            e.setValue(e.value - toExclude)
        }
    }

    override fun parametrize(): Sequence<Pair<Family, PrimitiveType?>> = buildSequence {
        for ((family, primitives) in familyPrimitives) {
            if (primitives.isEmpty())
                yield(family to null)
            else
                yieldAll(primitives.map { family to it })
        }
    }

    init {
        builderWith { p -> primitive = p }
    }
}

@TemplateDsl
class PairPrimitiveMemberDsl : GenericMemberDsl<Pair<PrimitiveType, PrimitiveType>>() {

    private val familyPrimitives = mutableMapOf<Family, Set<Pair<PrimitiveType, PrimitiveType>>>()

    fun include(f: Family, primitives: Collection<Pair<PrimitiveType, PrimitiveType>>) {
        familyPrimitives[f] = primitives.toSet()
    }

    override fun parametrize(): Sequence<Pair<Family, Pair<PrimitiveType, PrimitiveType>>> {
        return familyPrimitives
                .flatMap { e -> e.value.map { e.key to it } }
                .asSequence()
    }

    init {
        builderWith { (p1, p2) -> primitive = p1 }
    }
}


/*
val t_copyOfResized = MemberTemplatePar<DefaultParametrization>().apply {
    parametrization = buildSequence<DefaultParametrization> {
        val allPlatforms = setOf(*Platform.values())
        yield(DefaultParametrization(InvariantArraysOfObjects, platforms = allPlatforms))
        yieldAll(PrimitiveType.defaultPrimitives.map { DefaultParametrization(ArraysOfPrimitives, it, platforms = allPlatforms) })
    }
    builder = { p, platform, builder ->
        builder.family = if (p.family == InvariantArraysOfObjects && platform == Platform.JS)
            ArraysOfObjects else p.family

        if (platform == Platform.JVM)
            builder.inline = Inline.Only

        builder.doc = "Returns new array which is a copy of the original array."
        builder.returns = "SELF"
        if (platform == Platform.JS && p.family == ArraysOfObjects)
            builder.returns = "Array<T>"

        if (platform == Platform.JVM) {
            builder.body = "return java.util.Arrays.copyOf(this, size)"
        } else if (platform == Platform.JS) {
            when (p.primitive) {
                null ->
                    builder.body = "return this.asDynamic().slice()"
                PrimitiveType.Char, PrimitiveType.Boolean, PrimitiveType.Long ->
                    builder.body = "return withType(\"${p.primitive}Array\", this.asDynamic().slice())"
                else -> {
                    builder.annotations += """@Suppress("NOTHING_TO_INLINE")"""
                    builder.inline = Inline.Yes
                    builder.body = "return this.asDynamic().slice()"
                }
            }
        }


    }
}
interface MemberTemplate {

    fun instantiate(): Sequence<MemberInstance>
}

class MemberTemplatePar<TParametrization : Parametrization> : MemberTemplate {

    val keyword: String = "fun"

    lateinit var parametrization: Sequence<TParametrization>
    lateinit var builder: (TParametrization, Platform, MemberBuilder) -> Unit

    override fun instantiate(): Sequence<MemberInstance> =
            parametrization.flatMap {
                it.platforms.asSequence().map { p ->
                    val memberBuilder = MemberBuilder().apply {
                        builder(it, p, this)
                    }
                    MemberInstance(memberBuilder::build, PlatformSourceFile(p, memberBuilder.sourceFile))
                }
            }

}
*/



/*

class MemberInstance(
        val textBuilder: (Appendable) -> Unit,
        val platformSourceFile: PlatformSourceFile)
*/


