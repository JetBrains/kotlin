@_exported import ExportedKotlinPackages
import FooKit
@_implementationOnly import KotlinBridges_CinteropReexport
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.main {
    public static func consumesBar(
        x: any FooKit.Bar
    ) -> Swift.Int32 {
        return main_consumesBar__TypesOfArguments__anyU20FooKit_Bar__(x)
    }
    public static func consumesFoo(
        x: FooKit.Foo
    ) -> Swift.Int32 {
        return main_consumesFoo__TypesOfArguments__FooKit_Foo__(x)
    }
    public static func producesFoo() -> FooKit.Foo? {
        return main_producesFoo().map { it in it as! FooKit.Foo }
    }
}
