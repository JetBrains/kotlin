class Inv<E>
class Out<out T>
class OutPair<out Final, out Y>
class In<in Z>

class Final
open class Open

class Container {
    // The signatures are obtained from compiler/testData/writeSignature/declarationSiteVariance/wildcardOptimization
    fun openClassArgument(x: Out<Open>, y: In<Open>) {}
    fun finalClassArgument(x: Out<Final>, y: In<Final>) {}
    fun oneArgumentFinal(x: OutPair<Final, Open>) {}

    fun arrayOfOutOpen(x: Array<Out<Open>>) {}
    fun arrayOfOutFinal(x: Array<Out<Final>>) {}
    fun outOfArrayOpen(x: Out<Array<Open>>) {}
    fun outOfArrayOutOpen(x: Out<Array<out Open>>) {}

    fun deepOpen(x: Out<Out<Out<Open>>>) {}
    fun deepFinal(x: Out<Out<Out<Final>>>) {}

    fun skipAllOutInvWildcards(): Inv<OutPair<Open, Out<Out<Open>>>> = null!!
    fun skipAllInvWildcards(): Inv<In<Out<Open>>> = null!!
    fun notDeepIn(): In<Final> = null!!
    fun skipWildcardsUntilIn0(): Out<In<Out<Open>>> = null!!
    fun skipWildcardsUntilIn1(): Out<In<Out<Final>>> = null!!
    fun skipWildcardsUntilIn2(): Out<In<OutPair<Final, Out<Open>>>> = null!!
    fun skipWildcardsUntilInProjection(): Inv<in Out<Open>> = null!!

    fun outIn(x: Out<In<Final>>) {}
    fun outInAny(x: Out<In<Any?>>) {}

    fun invInv(x: Out<Inv<Open>>) {}
    fun invOut(x: Out<Inv<out Open>>) {}
    fun invOutFinal(x: Out<Inv<out Final>>) {}
    fun invIn(x: Out<Inv<in Final>>) {}
    fun invInAny(x: Out<Inv<in Any>>) {}

    fun inFinal(x: In<Final>) {}
    fun inAny(x: In<Any>) {}
    fun inOutFinal(x: In<Out<Final>>) {}

    fun invOpen(x: Inv<Open>) {}
    fun invFinal(x: Inv<Final>) {}
    fun invOutOpen(x: Inv<Out<Open>>) {}
    fun invOutFinal(x: Inv<Out<Final>>) {}
    fun invInOutOpen(x: Inv<In<Out<Open>>>) {}
    fun invInOutFinal(x: Inv<In<Out<Final>>>) {}
    fun invOutProjectedOutFinal(x: Inv<out Out<Final>>) {}

    fun <Q : Final> typeParameter(x: Out<Q>, y: In<Q>) {}
}
