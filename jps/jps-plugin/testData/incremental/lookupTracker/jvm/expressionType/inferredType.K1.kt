package foo

/*p:foo(A)*/open class A
/*p:foo*/class B : /*p:foo*/A()

/*p:foo*/fun getA() = /*p:foo*/A()
/*p:foo*/fun getB() = /*p:foo*/B()

/*p:foo*/fun getListOfA() = /*p:foo p:foo(A) p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.collections(List) p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/listOf(/*p:foo*/A())
/*p:foo*/fun getListOfB() = /*p:foo p:foo(B) p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.collections(List) p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/listOf(/*p:foo*/B())

/*p:foo*/fun useListOfA(a: /*p:foo p:kotlin.collections*/List</*p:foo*/A>) {}
/*p:foo*/fun useListOfB(b: /*p:foo p:kotlin.collections*/List</*p:foo*/B>) {}

/*p:foo*/fun testInferredType() {
    /*p:foo*/useListOfA(/*p:foo p:foo(A) p:kotlin.collections(List)*/getListOfA())
    /*p:foo*/useListOfA(/*p:foo p:foo(B) p:kotlin.collections(List)*/getListOfB())
    /*p:foo*/useListOfB(/*p:foo p:foo(B) p:kotlin.collections(List)*/getListOfB())
}
