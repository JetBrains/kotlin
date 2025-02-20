import Autoimport
import Testing

@Test
func test() throws {
    let descr = useAVFoundation()
    try #require(descr.count > 0)
}