import Main
import Testing
import Foundation

@Test
func testCallingKotlinThatUsesCoroutines() async throws {
    #expect(await testPrimitive() == 42)
    #expect(await testAny() as? Foo == Foo.shared)
    #expect(await testObject() == Foo.shared)
    #expect(await testCustom() == "Hello, World!")
}