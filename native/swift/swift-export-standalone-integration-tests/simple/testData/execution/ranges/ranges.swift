import Ranges
import KotlinRuntime
import Testing

@Test
func testSimple() throws {
    let d = distance(some: 5 ... 10)
    let m = materialize()
    let s = simple(some: 5 ... 10)
    #expect(d == 5)
    #expect(m.contains(8))
    #expect(s.contains(4))
    #expect(s.contains(11))
    #expect(!s.contains(0))
}

@Test
func nullable() throws {
    let n1 = nullable(some: -10 ... -5)
    let n2 = nullable(some: nil)
    #expect(n1 == -11 ... -4)
    #expect(n2 == nil)
}

@Test
func testTotal() throws {
    // Currently crashes with CCE
    //let t = total(list: [1 ... 5, 0 ... 4, 2 ... 7])
    //#expect(t == 0 ... 7)
}
