import InnerClass
import Testing

@Test
func testInnerClass() {
    let outer = Outer()
    let inner = Outer.Inner(outer__: outer)
    let innerInner = Outer.Inner.InnerInner(outer__: inner)
}

@Test
func testNestedInnerClassWithParameters() throws {
    let outer = OuterWithParam(outerParam: 0)
    let inner = OuterWithParam.InnerWithParam(innerParamA: 1, innerParamB: 2, outer__: outer)
    let innerInner = OuterWithParam.InnerWithParam.InnerInnerWithParam(innerInnerParam: 3, outer__: inner)
    try #require(innerInner.getOuter() == 0)
    try #require(innerInner.getInnerA() == 1)
    try #require(innerInner.getInnerB() == 2)
    try #require(innerInner.getInnerInner() == 3)
}