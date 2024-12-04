/*p:<root>*/fun Explicit() {
    /*p:<root> p:MainClass(Companion)*/MainClass./*p:MainClass*/Companion
}

/*p:<root>*/fun ExplicitMethod() {
    /*p:<root> p:<root>(f) p:MainClass(Companion) p:MainClass(f) p:MainClass.Companion(f)*/MainClass./*p:MainClass*/Companion.f()
}

/*p:<root>*/fun Implicit() {
    /*p:<root> p:MainClass(Companion)*/MainClass
}

/*p:<root>*/fun ImplicitMethod() {
    /*p:<root> p:<root>(f) p:MainClass(Companion) p:MainClass(f) p:MainClass.Companion(f)*/MainClass.f()
}

/*p:<root>*/fun InstanceExplicit() {
    val t = /*p:<root> p:MainClass(Companion)*/MainClass./*p:MainClass*/Companion
}

/*p:<root>*/fun Type(t: /*p:<root> p:MainClass(Companion)*/MainClass.Companion) {

}
