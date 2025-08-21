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


