package foo

/*p:foo(A)*/open class A
/*p:foo*/class B : /*p:foo*/A()

/*p:foo*/fun getA() = /*p:foo*/A()
/*p:foo*/fun getB() = /*p:foo*/B()

/*p:foo*/fun getListOfA() = /*p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:kotlin.jvm p:java.lang p:foo p:kotlin.collections(List) p:foo(A)*/listOf(/*p:foo*/A())
/*p:foo*/fun getListOfB() = /*p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:kotlin.jvm p:java.lang p:foo p:kotlin.collections(List) p:foo(B)*/listOf(/*p:foo*/B())

/*p:foo*/fun useListOfA(a: /*p:foo p:kotlin.collections*/List</*p:foo*/A>) {}
/*p:foo*/fun useListOfB(b: /*p:foo p:kotlin.collections*/List</*p:foo*/B>) {}

/*p:foo*/fun testInferredType() {
    /*p:foo*/useListOfA(/*p:foo p:kotlin.collections(List) p:foo(A)*/getListOfA())
    /*p:foo*/useListOfA(/*p:foo p:kotlin.collections(List) p:foo(B)*/getListOfB())
    /*p:foo*/useListOfB(/*p:foo p:kotlin.collections(List) p:foo(B)*/getListOfB())
}
