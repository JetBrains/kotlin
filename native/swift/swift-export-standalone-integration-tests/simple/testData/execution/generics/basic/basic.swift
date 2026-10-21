import Generics
import Testing
import KotlinRuntime

@Test
func smoke() throws {
    let foo = Foo(i: 5)
    try #require(foo == id(param: foo) as! KotlinBase)
    try #require(nil == id(param: nil))
}

@Test
func tripleBox() throws {
    let tb = TripleBox(i: 5)
    let db = tb.t as! Box
    try #require((((tb.t as! Box).t as! Box).t as! Foo).i == 5)
    try #require(((db.t as! Box).t as! Foo).i == 5)
    try #require(tb.unwrap() == 5)

    tb.set(newValue: 3)
    try #require(((db.t as! Box).t as! Foo).i == 3)
}

@Test
func primitiveBox() throws {
    let ib = IntBox(t: 5)
    try #require(ib.t == 5)
    ib.t = 3
    try #require(ib.t == 3)
}

@Test
func stringThroughId() throws {
    let result = id(param: "hello") as! String
    try #require(result == "hello")
}

@Test
func int32ThroughId() throws {
    let result = id(param: Int32(42)) as! Int32
    try #require(result == 42)
}

@Test
func arrayThroughId() throws {
    let result = id(param: [Int32(1), Int32(2), Int32(3)]) as? [Int32]
    try #require(result == [1, 2, 3])
}
