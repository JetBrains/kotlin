/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.domains

import org.jetbrains.kotlin.formver.domains.RuntimeTypeDomain.Companion.isOf
import org.jetbrains.kotlin.formver.viper.ast.*

/**
 * Produces set of axioms for injections from built-in types in Viper to Ref.
 *
 * @param viperType: built-in type which needs to be mapped
 * @param typeFunction: representation of that type as a domain func
 */
class Injection(
    injectionName: String,
    val viperType: Type,
    val typeFunction: DomainFunc
) {
    private val v = Var("v", viperType)
    private val r = Var("r", Type.Ref)
    val toRef = RuntimeTypeDomain.createDomainFunc("${injectionName}ToRef", listOf(v.decl()), Type.Ref)
    val fromRef = RuntimeTypeDomain.createDomainFunc("${injectionName}FromRef", listOf(r.decl()), viperType)

    internal fun AxiomListBuilder.injectionAxioms() {
        axiom {
            Exp.forall(v) { v -> simpleTrigger { toRef(v) isOf typeFunction() } }
        }
        axiom {
            Exp.forall(v) { v ->
                simpleTrigger { fromRef(toRef(v)) } eq v
            }
        }
        axiom {
            Exp.forall(r) { r ->
                assumption { r isOf typeFunction() }
                simpleTrigger { toRef(fromRef(r)) } eq r
            }
        }
    }
}