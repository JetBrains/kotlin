/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.scala.silicon.ast

import org.jetbrains.kotlin.formver.embeddings.DomainAxiomName
import org.jetbrains.kotlin.formver.embeddings.DomainFuncName
import org.jetbrains.kotlin.formver.embeddings.DomainName
import org.jetbrains.kotlin.formver.scala.IntoViper
import org.jetbrains.kotlin.formver.scala.toScalaOption
import org.jetbrains.kotlin.formver.scala.toScalaSeq
import viper.silver.ast.AnonymousDomainAxiom
import viper.silver.ast.LocalVarDecl
import viper.silver.ast.NamedDomainAxiom

// Cannot implement `IntoViper` as we need to pass the domain name.
class DomainFunc(
    val name: DomainFuncName,
    val formalArgs: List<LocalVarDecl>,
    val typ: Type,
    val unique: Boolean,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) {
    fun toViper(domainName: String): viper.silver.ast.DomainFunc =
        viper.silver.ast.DomainFunc(
            name.mangled,
            formalArgs.toScalaSeq(),
            typ.toViper(),
            unique,
            null.toScalaOption(),
            pos.toViper(),
            info.toViper(),
            domainName,
            trafos.toViper()
        )
}

// Cannot implement `IntoViper` as we need to pass the domain name.
class DomainAxiom(
    val name: DomainAxiomName?,
    val exp: Exp,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) {
    fun toViper(domainName: String): viper.silver.ast.DomainAxiom =
        if (name != null)
            NamedDomainAxiom(name.mangled, exp.toViper(), pos.toViper(), info.toViper(), domainName, trafos.toViper())
        else
            AnonymousDomainAxiom(exp.toViper(), pos.toViper(), info.toViper(), domainName, trafos.toViper())
}

abstract class Domain(
    val name: DomainName,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoViper<viper.silver.ast.Domain> {

    abstract val typeVars: List<Type.TypeVar>
    abstract val functions: List<DomainFunc>
    abstract val axioms: List<DomainAxiom>

    override fun toViper(): viper.silver.ast.Domain =
        viper.silver.ast.Domain(
            name.mangled,
            functions.map { it.toViper(name.mangled) }.toScalaSeq(),
            axioms.map { it.toViper(name.mangled) }.toScalaSeq(),
            typeVars.map { it.toViper() }.toScalaSeq(),
            null.toScalaOption(),
            pos.toViper(),
            info.toViper(),
            trafos.toViper()
        )

    fun toType(typeParamSubst: Map<Type.TypeVar, Type> = typeVars.associateWith { it }): Type.Domain =
        Type.Domain(name.mangled, typeVars, typeParamSubst)

    fun createDomainFunc(funcName: String, args: List<LocalVarDecl>, type: Type, unique: Boolean = false) =
        DomainFunc(DomainFuncName(this.name, funcName), args, type, unique)

    fun createDomainAxiom(axiomName: String?, exp: Exp): DomainAxiom =
        DomainAxiom(axiomName?.let { DomainAxiomName(this.name, it) }, exp)

    fun funcApp(
        func: DomainFunc,
        args: List<Exp>,
        typeVarMap: Map<Type.TypeVar, Type> = typeVars.associateWith { it },
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): Exp.DomainFuncApp = Exp.DomainFuncApp(name.mangled, func.name.mangled, args, typeVarMap, func.typ, pos, info, trafos)
}
