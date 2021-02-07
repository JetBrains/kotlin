//ALLOW_AST_ACCESS
package test

class MembersReferenceOuterTP<P> {
    inner class Inner {
        fun <Q : P> f() {}
        fun g(p: P): P = null!!

        val v: P = null!!
        val <Q : P> Q.w: Q get() = null!!
    }
}
