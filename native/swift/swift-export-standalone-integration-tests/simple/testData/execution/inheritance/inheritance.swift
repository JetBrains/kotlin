import Inheritance
import Testing

@Test
func inhertianceIsForbidden() throws {
    class Derived: Base {}

    // This will lead to a crash
    Derived()
}