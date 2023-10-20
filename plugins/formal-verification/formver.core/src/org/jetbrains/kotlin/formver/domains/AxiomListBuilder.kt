/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.domains

import org.jetbrains.kotlin.formver.viper.ast.Domain
import org.jetbrains.kotlin.formver.viper.ast.DomainAxiom
import org.jetbrains.kotlin.formver.viper.ast.Exp

/**
 * Helper class for constructing axioms.
 *
 * Should be used only through the `build` function, as in `AxiomsBuilder.build(...) { ... }`.
 *
 * Note that this class needs access to the domain in which the axioms will reside; this is because
 * Viper requires axioms to be annotated with the domain name and type variable information. We
 * additionally perform name mangling based on the domain name, so all in all this setup is easier
 * than trying to get all the necessary info here separately.
 *
 * The main convenience we get from this is easier access to the `forall` helper function for
 * expressions, as well as the ability to emit axioms from a loop.
 *
 * Doing the same thing for domains as a whole is tempting but impractical: domain functions
 * should be members of their respective domains, which makes creating them from a DSL unwieldy.
 */
internal class AxiomListBuilder(private val domain: Domain) {
    val axioms = mutableListOf<DomainAxiom>()

    companion object {
        fun build(domain: Domain, action: AxiomListBuilder.() -> Unit): List<DomainAxiom> {
            val builder = AxiomListBuilder(domain)
            builder.action()
            return builder.axioms
        }
    }

    /**
     * Add a new named axiom with the body given by the action.
     */
    fun axiom(name: String, action: () -> Exp): DomainAxiom {
        val exp = action()
        val axiom = domain.createNamedDomainAxiom(name, exp)
        axioms.add(axiom)
        return axiom
    }

    /**
     * Add a new anonymous axiom with the body given by the action.
     */
    fun axiom(action: () -> Exp): DomainAxiom {
        val exp = action()
        val axiom = domain.createAnonymousDomainAxiom(exp)
        axioms.add(axiom)
        return axiom
    }
}
