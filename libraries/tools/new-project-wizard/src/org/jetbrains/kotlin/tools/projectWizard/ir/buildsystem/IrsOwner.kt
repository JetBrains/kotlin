package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem


interface IrsOwner {
    val irs: List<BuildSystemIR>
    fun withReplacedIrs(irs: List<BuildSystemIR>): IrsOwner
}

@Suppress("UNCHECKED_CAST")
fun <I : IrsOwner> I.withIrs(irs: List<BuildSystemIR>) = withReplacedIrs(irs = this.irs + irs) as I

@Suppress("UNCHECKED_CAST")
fun <I : IrsOwner> I.withIrs(vararg irs: BuildSystemIR) = withReplacedIrs(irs = this.irs + irs) as I


inline fun <reified I : BuildSystemIR> IrsOwner.irsOfType(): List<I> =
    irs.filterIsInstance<I>()


inline fun <reified I : BuildSystemIR> IrsOwner.irsOfTypeOrNull() =
    irsOfType<I>().takeIf { it.isNotEmpty() }


inline fun <reified T : BuildSystemIR> IrsOwner.firstIrOfType() =
    irs.firstOrNull { ir -> ir is T } as T?

fun IrsOwner.freeIrs(): List<FreeIR> =
    irs.filterIsInstance<FreeIR>()
