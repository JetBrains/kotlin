/*p:<root>*/fun Explicit() /*p:MainClass(Companion)*/{
    /*p:<root> p:MainClass(Companion)*/MainClass./*p:MainClass p:MainClass.Companion*/Companion
}

/*p:<root>*/fun ExplicitMethod() {
    /*p:<root>*/MainClass./*p:MainClass*/Companion./*p:MainClass.Companion*/f()
}

/*p:<root>*/fun Implicit() /*p:MainClass(Companion)*/{
    /*p:<root> p:MainClass(Companion)*/MainClass
}

/*p:<root>*/fun ImplicitMethod() {
    /*p:<root>*/MainClass./*p:MainClass p:MainClass.Companion*/f()
}

/*p:<root>*/fun InstanceExplicit() {
    val t = /*p:<root> p:MainClass(Companion)*/MainClass./*p:MainClass p:MainClass.Companion*/Companion
}

/*p:<root>*/fun Type(t: /*p:<root>*/MainClass./*p:MainClass*/Companion) {

}
