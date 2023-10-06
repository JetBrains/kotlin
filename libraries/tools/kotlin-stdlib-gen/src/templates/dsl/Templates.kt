/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package templates

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

fun fn(defaultSignature: String, setup: FamilyPrimitiveMemberDefinition.() -> Unit): FamilyPrimitiveMemberDefinition =
        FamilyPrimitiveMemberDefinition().apply {
            builder(fn(defaultSignature))
            setup()
        }

fun MemberBuildAction.byTwoPrimitives(setup: PairPrimitiveMemberDefinition.() -> Unit): PairPrimitiveMemberDefinition =
        PairPrimitiveMemberDefinition().apply {
            builder(this@byTwoPrimitives)
            setup()
        }

fun pval(name: String, setup: FamilyPrimitiveMemberDefinition.() -> Unit): FamilyPrimitiveMemberDefinition =
        FamilyPrimitiveMemberDefinition().apply {
            builder(def(name, Keyword.Value))
            setup()
        }

fun pvar(name: String, setup: FamilyPrimitiveMemberDefinition.() -> Unit): FamilyPrimitiveMemberDefinition =
        FamilyPrimitiveMemberDefinition().apply {
            builder(def(name, Keyword.Variable))
            setup()
        }


interface MemberTemplate {
    /** Specifies which platforms this member template should be generated for */
    fun platforms(vararg platforms: Platform)

    fun instantiate(targets: Collection<KotlinTarget> = KotlinTarget.entries): Sequence<MemberBuilder>

    /** Registers parameterless member builder function */
    fun builder(b: MemberBuildAction)
}

infix fun <MT: MemberTemplate> MT.builder(b: MemberBuildAction): MT = apply { builder(b) }
infix fun <TParam, MT : MemberTemplateDefinition<TParam>> MT.builderWith(b: MemberBuildActionP<TParam>): MT = apply { builderWith(b) }

abstract class MemberTemplateDefinition<TParam> : MemberTemplate {

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

    private val buildActions = mutableListOf<BuildAction>()

    private var allowedPlatforms = Platform.entries.toSet()
    override fun platforms(vararg platforms: Platform) {
        allowedPlatforms = setOf(*platforms)
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


    override fun instantiate(targets: Collection<KotlinTarget>): Sequence<MemberBuilder> {
        val resultingTargets = targets.filter { it.platform in allowedPlatforms }
        val resultingPlatforms = resultingTargets.map { it.platform }.distinct()
        val specificTargets by lazy { resultingTargets - KotlinTarget.Common }

        fun platformMemberBuilders(family: Family, p: TParam) =
                if (Platform.Common in allowedPlatforms) {
                    val commonMemberBuilder = createMemberBuilder(KotlinTarget.Common, family, p)
                    mutableListOf<MemberBuilder>().also { builders ->
                        if (Platform.Common in resultingPlatforms) builders.add(commonMemberBuilder)
                        if (commonMemberBuilder.hasPlatformSpecializations) {
                            specificTargets.mapTo(builders) {
                                createMemberBuilder(it, family, p)
                            }
                        }
                    }
                } else {
                    resultingTargets.map { createMemberBuilder(it, family, p) }
                }

        return parametrize()
                .applyFilter()
                .map { (family, p) -> platformMemberBuilders(family, p) }
                .flatten()
    }

    private fun createMemberBuilder(target: KotlinTarget, family: Family, p: TParam): MemberBuilder {
        return MemberBuilder(allowedPlatforms, target, family).also { builder ->
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
    when {
        f == Family.Unsigned || f == Family.ArraysOfUnsigned -> PrimitiveType.unsignedPrimitives
        f == Family.RangesOfPrimitives -> PrimitiveType.rangePrimitives
        f.isPrimitiveSpecialization -> PrimitiveType.defaultPrimitives
        else -> emptySet()
    }

@TemplateDsl
class FamilyPrimitiveMemberDefinition : MemberTemplateDefinition<PrimitiveType?>() {

    private val familyPrimitives = mutableMapOf<Family, Set<PrimitiveType?>>()

    fun include(vararg fs: Family) {
        for (f in fs) familyPrimitives[f] = defaultPrimitives(f)
    }
    @Deprecated("Use include()", ReplaceWith("include(*fs)"))
    fun only(vararg fs: Family) = include(*fs)

    fun include(fs: Collection<Family>) {
        for (f in fs) familyPrimitives[f] = defaultPrimitives(f)
    }

    fun includeDefault() {
        include(Family.defaultFamilies)
    }

    fun include(f: Family, primitives: Set<PrimitiveType?>) {
        familyPrimitives[f] = primitives
    }

    fun exclude(vararg ps: PrimitiveType) {
        val toExclude = ps.toSet()
        for (e in familyPrimitives) {
            e.setValue(e.value - toExclude)
        }
    }

    override fun parametrize(): Sequence<Pair<Family, PrimitiveType?>> = sequence {
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
class PairPrimitiveMemberDefinition : MemberTemplateDefinition<Pair<PrimitiveType, PrimitiveType>>() {

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
        builderWith { (p1, _) -> primitive = p1 }
    }
}

/*
Replacement pattern:
    templates add f\(\"(\w+)(\(.*)
    val f_$1 = fn("$1$2
*/

