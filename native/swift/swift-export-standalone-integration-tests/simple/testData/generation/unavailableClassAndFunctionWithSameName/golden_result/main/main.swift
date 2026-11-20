@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.com.github.jetbrains.swiftexport.se {
    @available(*, unavailable, message: "Use BannerModel")
    public final class Banner: KotlinRuntime.KotlinBase {
        public var text: Swift.String {
            get {
                return com_github_jetbrains_swiftexport_se_Banner_text_get(self.__externalRCRef())
            }
        }
        public init(
            text: Swift.String
        ) {
            let __kt = com_github_jetbrains_swiftexport_se_Banner_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { com_github_jetbrains_swiftexport_se_Banner_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(__kt, text); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    @available(*, unavailable, message: "Unavailable type(s): ExportedKotlinPackages.com.github.jetbrains.swiftexport.se.Banner")
    public static func banner(
        text: Swift.String,
        dummy: Swift.Int32
    ) -> ExportedKotlinPackages.com.github.jetbrains.swiftexport.se.Banner {
        fatalError()
    }
    @available(*, unavailable, message: "Use loadBannerModel")
    public static func loadBanner() -> ExportedKotlinPackages.com.github.jetbrains.swiftexport.se.Banner {
        fatalError()
    }
}
