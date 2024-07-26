/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.*
import viper.silver.ast.AnonymousDomainAxiom
import viper.silver.ast.NamedDomainAxiom

/**
 * We also convert domain names and their function and axiom names as
 * they have to be globally unique as well.
 */
data class DomainName(val name: String) : MangledName {
    // Info: Can't use 'domain' as prefix as Viper recognizes it as a keyword
    override val mangled: String = "dom\$$name"
}

data class DomainFuncName(val domainName: DomainName, val funcName: String) : MangledName {
    override val mangled: String = "${domainName.mangled}\$${funcName}"
}

/** Represents the name of a possible anonymous axiom.
 *
 * We need the domain name regardless because of how Viper is set up, hence the somewhat
 * unusual
 */
sealed interface OptionalDomainAxiomLabel {
    val domainName: DomainName
}

data class NamedDomainAxiomLabel(override val domainName: DomainName, val axiomName: String) : OptionalDomainAxiomLabel, MangledName {
    override val mangled: String = "${domainName.mangled}\$${axiomName}"
}

data class AnonymousDomainAxiomLabel(override val domainName: DomainName) : OptionalDomainAxiomLabel

class DomainFunc(
    val name: DomainFuncName,
    val formalArgs: List<Declaration.LocalVarDecl>,
    val typeArgs: List<Type.TypeVar>,
    val returnType: Type,
    val unique: Boolean,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoSilver<viper.silver.ast.DomainFunc> {
    override fun toSilver(): viper.silver.ast.DomainFunc =
        viper.silver.ast.DomainFunc(
            name.mangled,
            formalArgs.map { it.toSilver() }.toScalaSeq(),
            returnType.toSilver(),
            unique,
            null.toScalaOption(),
            pos.toSilver(),
            info.toSilver(),
            name.domainName.mangled,
            trafos.toSilver()
        )

    operator fun invoke(vararg args: Exp): Exp.DomainFuncApp =
        Exp.DomainFuncApp(this, args.toList(), typeArgs.associateWith { it })
}

class DomainAxiom(
    val name: OptionalDomainAxiomLabel,
    val exp: Exp,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoSilver<viper.silver.ast.DomainAxiom> {
    override fun toSilver(): viper.silver.ast.DomainAxiom =
        when (name) {
            is NamedDomainAxiomLabel -> NamedDomainAxiom(
                name.mangled,
                exp.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                name.domainName.mangled,
                trafos.toSilver()
            )
            is AnonymousDomainAxiomLabel -> AnonymousDomainAxiom(
                exp.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                name.domainName.mangled,
                trafos.toSilver()
            )
        }
}

abstract class Domain(
    baseName: String,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoSilver<viper.silver.ast.Domain> {
    val name = DomainName(baseName)

    open val includeInShortDump: Boolean = true
    abstract val typeVars: List<Type.TypeVar>
    abstract val functions: List<DomainFunc>
    abstract val axioms: List<DomainAxiom>

    override fun toSilver(): viper.silver.ast.Domain =
        viper.silver.ast.Domain(
            name.mangled,
            functions.toSilver().toScalaSeq(),
            axioms.toSilver().toScalaSeq(),
            // Can't use List.toViper directly here as the type would end up being `List<Type>` instead of `List<TypeVar`.
            typeVars.map { it.toSilver() }.toScalaSeq(),
            null.toScalaOption(),
            pos.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )

    // Don't use this directly, instead, use the custom types defined in `org.jetbrains.kotlin.formver.viper.ast.Type` for specific domains.
    fun toType(typeParamSubst: Map<Type.TypeVar, Type> = typeVars.associateWith { it }): Type.Domain =
        Type.Domain(name.mangled, typeVars, typeParamSubst)

    fun createDomainFunc(funcName: String, args: List<Declaration.LocalVarDecl>, type: Type, unique: Boolean = false) =
        DomainFunc(DomainFuncName(this.name, funcName), args, typeVars, type, unique)

    fun createNamedDomainAxiom(axiomName: String, exp: Exp): DomainAxiom =
        DomainAxiom(NamedDomainAxiomLabel(this.name, axiomName), exp)

    fun createAnonymousDomainAxiom(exp: Exp): DomainAxiom = DomainAxiom(AnonymousDomainAxiomLabel(this.name), exp)

    fun funcApp(
        func: DomainFunc,
        args: List<Exp>,
        typeVarMap: Map<Type.TypeVar, Type> = typeVars.associateWith { it },
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): Exp.DomainFuncApp = Exp.DomainFuncApp(func, args, typeVarMap, pos, info, trafos)
}

abstract class BuiltinDomain(
    baseName: String,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
    trafos: Trafos = Trafos.NoTrafos,
) : Domain(baseName, pos, info, trafos) {
    override val includeInShortDump: Boolean = false
}