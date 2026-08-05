import Inheritance
import Testing

@Test
func swiftOverrideCanCallSuperOnKotlinClass() throws {
    // A Swift override that calls `super.method()` must reach the inherited Kotlin implementation
    // via the non-virtual ("direct dispatch") forward bridge, instead of re-entering the patched
    // vtable slot and recursing forever.
    class FancyVehicle: Vehicle {
        override func describe() -> String {
            return "fancy-" + super.describe()
        }
    }
    let v = FancyVehicle()

    // Direct Swift dispatch: the override runs and its super-call lands in Kotlin.
    #expect(v.describe() == "fancy-kotlin-vehicle")
    // Kotlin-side dispatch reaches the Swift override, whose super-call again lands in Kotlin.
    #expect(callDescribe(v: v) == "fancy-kotlin-vehicle")

    // Original Kotlin instance is unaffected.
    #expect(callDescribe(v: Vehicle()) == "kotlin-vehicle")
    #expect(Vehicle().describe() == "kotlin-vehicle")
}

@Test
func swiftSubclassInheritsNonOverriddenKotlinMethod() throws {
    // A Swift subclass that overrides only some methods must still be able to invoke the
    // non-overridden ones (whose vtable slots are also patched) without infinite recursion.
    class FancyVehicle: Vehicle {
        override func describe() -> String { "fancy" }
        // `wheels()` is intentionally not overridden.
    }
    let v = FancyVehicle()

    #expect(v.describe() == "fancy")
    // Inherited, non-overridden method via direct Swift dispatch.
    #expect(v.wheels() == 4)
    // Inherited, non-overridden method reached through a Kotlin caller must not recurse.
    #expect(callWheels(v: v) == 4)
    #expect(callDescribe(v: v) == "fancy")
}
