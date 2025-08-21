import LibraryA
import LibraryB
import Testing

@Test
func test() throws {
    let a = MyLibraryA()
    try #require(a.returnMe() == a)
}