@_implementationOnly import KotlinBridges_main_three
import dependency_deeper_neighbor_exported

public typealias Foo = ExportedKotlinPackages.dependency.four.AnotherBar
public var deps_instance_3: main_three.Foo {
    get {
        return ExportedKotlinPackages.dependency.four.AnotherBar(__externalRCRef: __root___deps_instance_3_get())
    }
}
