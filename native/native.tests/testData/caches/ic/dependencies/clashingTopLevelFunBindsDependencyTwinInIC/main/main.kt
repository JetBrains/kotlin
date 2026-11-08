import kotlin.test.*

@Test
fun runTest() {
    // callF() must call user's own f() -- the declaration-site semantics, which the non-IC
    // build implements and asserts here (10). Today every IC flavor (clean, default,
    // unbatched) binds p/f to lib's twin first and this fails with
    // "Expected <10>, actual <20>".
    assertEquals(10, p.callF())
}
