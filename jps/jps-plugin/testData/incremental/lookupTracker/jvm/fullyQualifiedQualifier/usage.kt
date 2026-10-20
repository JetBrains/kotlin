package usage

/*p:usage*/fun test() {
    /*p:lib(O) p:lib.O(m) p:usage p:usage(m)*/lib./*p:lib*/O.m()
    /*p:lib(A) p:lib.A(Companion) p:lib.A(c) p:lib.A.Companion(c) p:usage p:usage(c)*/lib./*p:lib.A(Companion)*/A.c()
    /*p:lib(J) p:lib.J(s) p:usage*/lib.J.s()
}

