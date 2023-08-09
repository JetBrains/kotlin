/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.scala.silicon.ast

import org.jetbrains.kotlin.formver.conversion.ConvertedType
import org.jetbrains.kotlin.formver.conversion.ConvertedVar
import org.jetbrains.kotlin.formver.scala.IntoViper
import org.jetbrains.kotlin.formver.scala.emptySeq
import org.jetbrains.kotlin.formver.scala.toScalaOption
import org.jetbrains.kotlin.formver.scala.toScalaSeq
import viper.silver.ast.AnonymousDomainAxiom
import viper.silver.ast.NamedDomainAxiom

// Cannot implement `IntoViper` as we need to pass the domain name.
class DomainFunc(
    val name: String,
    val formalArgs: List<ConvertedVar>,
    val typ: ConvertedType,
    val unique: Boolean,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) {
    fun toViper(domainName: String): viper.silver.ast.DomainFunc =
        viper.silver.ast.DomainFunc(
            name,
            formalArgs.map { it.toLocalVarDecl() }.toScalaSeq(),
            typ.viperType.toViper(),
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
    val name: String?,
    val exp: Exp,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) {
    fun toViper(domainName: String): viper.silver.ast.DomainAxiom =
        if (name != null)
            NamedDomainAxiom(name, exp.toViper(), pos.toViper(), info.toViper(), domainName, trafos.toViper())
        else
            AnonymousDomainAxiom(exp.toViper(), pos.toViper(), info.toViper(), domainName, trafos.toViper())
}

class Domain(
    val name: String,
    val functions: List<DomainFunc>,
    val axioms: List<DomainAxiom>,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoViper<viper.silver.ast.Domain> {
    override fun toViper(): viper.silver.ast.Domain =
        viper.silver.ast.Domain(
            name,
            functions.map { it.toViper(name) }.toScalaSeq(),
            axioms.map { it.toViper(name) }.toScalaSeq(),
            emptySeq(),
            null.toScalaOption(),
            pos.toViper(),
            info.toViper(),
            trafos.toViper()
        )

    fun toType(): Type.Domain = Type.Domain(name)
}
