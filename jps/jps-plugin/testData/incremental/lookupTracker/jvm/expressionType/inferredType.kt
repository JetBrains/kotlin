package foo

/*p:foo(A)*/open class A
/*p:foo*/class B : /*p:foo*/A()

/*p:foo*/fun getA() = /*p:foo*/A()
/*p:foo*/fun getB() = /*p:foo*/B()

/*p:foo*/fun getListOfA() = /*p:foo p:foo(A) p:kotlin.collections p:kotlin.collections(List)*/listOf(/*p:foo*/A())
/*p:foo*/fun getListOfB() = /*p:foo p:foo(B) p:kotlin.collections p:kotlin.collections(List)*/listOf(/*p:foo*/B())

/*p:foo*/fun useListOfA(a: /*p:foo p:foo(A) p:kotlin.collections*/List</*p:foo*/A>) {}
/*p:foo*/fun useListOfB(b: /*p:foo p:foo(B) p:kotlin.collections*/List</*p:foo*/B>) {}

/*p:foo*/fun testInferredType() {
    /*p:foo*/useListOfA(/*p:foo p:foo(A) p:kotlin.collections(List)*/getListOfA())
    /*p:foo*/useListOfA(/*p:foo p:foo(B) p:kotlin.collections(List)*/getListOfB())
    /*p:foo*/useListOfB(/*p:foo p:foo(B) p:kotlin.collections(List)*/getListOfB())
}
