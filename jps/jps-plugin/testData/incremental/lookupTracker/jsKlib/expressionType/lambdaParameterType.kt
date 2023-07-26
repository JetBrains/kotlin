package foo

/*p:foo*/class C

/*p:foo*/fun lambdaConsumer(fn: (/*p:foo*/A)->/*p:foo*/Unit) {}
/*p:foo*/fun extensionConsumer(fn: /*p:foo*/A.()->/*p:foo*/Unit) {}

/*p:foo*/fun testLambdaParameterType() {
    /*p:foo p:kotlin(Unit)*/lambdaConsumer /*p:foo(A) p:kotlin(Function1) p:kotlin(Unit)*/{ /*p:kotlin(Unit)*/it }
    /*p:foo p:kotlin(Unit)*/extensionConsumer /*p:foo(A) p:kotlin(Function1) p:kotlin(Unit)*/{ /*p:kotlin(Unit)*/this }
}
