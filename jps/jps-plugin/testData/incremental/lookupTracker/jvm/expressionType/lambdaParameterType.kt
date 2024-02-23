package foo

/*p:foo*/class C

/*p:foo*/fun lambdaConsumer(fn: (/*p:foo*/A)->/*p:foo*/Unit) {}
/*p:foo*/fun extensionConsumer(fn: /*p:foo*/A.()->/*p:foo*/Unit) {}

/*p:foo*/fun testLambdaParameterType() {
    /*p:foo*/lambdaConsumer /*p:foo(A) p:kotlin(Function1)*/{ it }
    /*p:foo*/extensionConsumer /*p:foo(A) p:kotlin(Function1)*/{ this }
}
