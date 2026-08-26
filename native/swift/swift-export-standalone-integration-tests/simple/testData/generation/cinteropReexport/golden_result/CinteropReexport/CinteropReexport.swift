#if canImport(BarKit)
import BarKit
#endif
@_exported import ExportedKotlinPackages
#if canImport(FooKit)
import FooKit
#endif
@_implementationOnly import KotlinBridges_CinteropReexport
import KotlinRuntime
import KotlinRuntimeSupport
#if canImport(ZarKit)
import ZarKit
#endif

extension ExportedKotlinPackages.main {
    @discardableResult
    public static func consumesBar(
        x: any Zar
    ) -> Swift.Int32 {
        return main_consumesBar__TypesOfArguments__anyU20Zar__(x)
    }
    @discardableResult
    public static func consumesBar(
        x: any Zarable
    ) -> any Zarable {
        return main_consumesBar__TypesOfArguments__anyU20Zarable__(x) as! any Zarable
    }
    @discardableResult
    public static func consumesBar(
        x: Bar
    ) -> Swift.Int32 {
        return main_consumesBar__TypesOfArguments__Bar__(x)
    }
    @discardableResult
    public static func consumesFoo(
        x: Foo
    ) -> Swift.Int32 {
        return main_consumesFoo__TypesOfArguments__Foo__(x)
    }
    @discardableResult
    public static func producesFoo() -> Foo? {
        return main_producesFoo().map { it in it as! Foo }
    }
}
