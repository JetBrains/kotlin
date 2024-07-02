@_exported import ExportedKotlinPackages
import KotlinBridges_main2

public extension ExportedKotlinPackages.demo.shared {
    public static func foo2() -> Swift.Int32 {
        return demo_shared_foo2()
    }
}
