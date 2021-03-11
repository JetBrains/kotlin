package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toPersistentList


interface IrsOwner {
    val irs: PersistentList<BuildSystemIR>
    fun withReplacedIrs(irs: PersistentList<BuildSystemIR>): IrsOwner
}

@Suppress("UNCHECKED_CAST")
fun <I : IrsOwner> I.withIrs(irs: List<BuildSystemIR>) = withReplacedIrs(irs = this.irs + irs.toPersistentList()) as I

@Suppress("UNCHECKED_CAST")
fun <I : IrsOwner> I.withIrs(vararg irs: BuildSystemIR) = withReplacedIrs(irs = this.irs + irs) as I

@Suppress("UNCHECKED_CAST")
inline fun <I : IrsOwner> I.withoutIrs(filterNot: (BuildSystemIR) -> Boolean): I =
    withReplacedIrs(irs = this.irs.filterNot(filterNot).toPersistentList()) as I


inline fun <reified I : BuildSystemIR> IrsOwner.irsOfType(): List<I> =
    irs.filterIsInstance<I>().let { irs ->
        if (irs.all { it is BuildSystemIRWithPriority })
            irs.sortedBy { (it as BuildSystemIRWithPriority).priority }
        else irs
    }


inline fun <reified I : BuildSystemIR> IrsOwner.irsOfTypeOrNull() =
    irsOfType<I>().takeIf { it.isNotEmpty() }

inline fun <reified I : BuildSystemIR> List<BuildSystemIR>.irsOfTypeOrNull() =
    filterIsInstance<I>().takeIf { it.isNotEmpty() }


inline fun <reified T : BuildSystemIR> IrsOwner.firstIrOfType() =
    irs.firstOrNull { ir -> ir is T } as T?

fun IrsOwner.freeIrs(): List<FreeIR> =
    irs.filterIsInstance<FreeIR>()
