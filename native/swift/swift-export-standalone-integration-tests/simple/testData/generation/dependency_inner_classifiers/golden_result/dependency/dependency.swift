@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_dependency
import KotlinRuntime
import KotlinRuntimeSupport

public protocol _ExportedKotlinPackages_datetime_DateTimeFormatBuilder_WithDate: KotlinRuntime.KotlinBase, dependency.__ExportedKotlinPackages_datetime_DateTimeFormatBuilder_WithDate {
}
@objc(__ExportedKotlinPackages_datetime_DateTimeFormatBuilder_WithDate)
public protocol __ExportedKotlinPackages_datetime_DateTimeFormatBuilder_WithDate {
}
public protocol ___ExportedKotlinPackages_datetime_DateTimeFormatBuilder_WithDate: KotlinRuntimeSupport._KotlinBridgeable {
}
extension ExportedKotlinPackages.datetime.DateTimeFormat where Self : ExportedKotlinPackages.datetime.__DateTimeFormat {
}
extension ExportedKotlinPackages.datetime.DateTimeFormat {
}
extension ExportedKotlinPackages.datetime.DateTimeFormatBuilder where Self : ExportedKotlinPackages.datetime.__DateTimeFormatBuilder {
}
extension ExportedKotlinPackages.datetime.DateTimeFormatBuilder {
    typealias WithDate = dependency._ExportedKotlinPackages_datetime_DateTimeFormatBuilder_WithDate
}
extension dependency._ExportedKotlinPackages_datetime_DateTimeFormatBuilder_WithDate where Self : dependency.___ExportedKotlinPackages_datetime_DateTimeFormatBuilder_WithDate {
}
extension dependency._ExportedKotlinPackages_datetime_DateTimeFormatBuilder_WithDate {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.datetime.DateTimeFormat, ExportedKotlinPackages.datetime.__DateTimeFormat where Wrapped : ExportedKotlinPackages.datetime._DateTimeFormat {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.datetime.DateTimeFormatBuilder, ExportedKotlinPackages.datetime.__DateTimeFormatBuilder where Wrapped : ExportedKotlinPackages.datetime._DateTimeFormatBuilder {
}
extension KotlinRuntimeSupport._KotlinExistential: dependency._ExportedKotlinPackages_datetime_DateTimeFormatBuilder_WithDate, dependency.___ExportedKotlinPackages_datetime_DateTimeFormatBuilder_WithDate where Wrapped : dependency.__ExportedKotlinPackages_datetime_DateTimeFormatBuilder_WithDate {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.datetime._DateTimeFormat {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.datetime._DateTimeFormatBuilder {
}
extension KotlinRuntimeSupport._KotlinExistentialPenBox: dependency.__ExportedKotlinPackages_datetime_DateTimeFormatBuilder_WithDate {
}
extension ExportedKotlinPackages.datetime {
    public protocol DateTimeFormat: KotlinRuntime.KotlinBase, ExportedKotlinPackages.datetime._DateTimeFormat {
    }
    public protocol DateTimeFormatBuilder: KotlinRuntime.KotlinBase, ExportedKotlinPackages.datetime._DateTimeFormatBuilder {
    }
    @objc(_DateTimeFormat)
    public protocol _DateTimeFormat {
    }
    @objc(_DateTimeFormatBuilder)
    public protocol _DateTimeFormatBuilder {
    }
    public protocol __DateTimeFormat: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol __DateTimeFormatBuilder: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public final class LocalDate: KotlinRuntime.KotlinBase {
        public final class Companion: KotlinRuntime.KotlinBase {
            public static var shared: ExportedKotlinPackages.datetime.LocalDate.Companion {
                get {
                    return ExportedKotlinPackages.datetime.LocalDate.Companion.__createClassWrapper(externalRCRef: datetime_LocalDate_Companion_get())
                }
            }
            private init() {
                fatalError()
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
            }
            public func Format(
                block: @escaping (any dependency._ExportedKotlinPackages_datetime_DateTimeFormatBuilder_WithDate) -> Swift.Void
            ) -> any ExportedKotlinPackages.datetime.DateTimeFormat {
                return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: datetime_LocalDate_Companion_Format__TypesOfArguments__U28anyU20dependency__ExportedKotlinPackages_datetime_DateTimeFormatBuilder_WithDateU29202D_U20Swift_Void__(self.__externalRCRef(), {
                    let originalBlock: (any dependency._ExportedKotlinPackages_datetime_DateTimeFormatBuilder_WithDate) -> Swift.Void = block
                    return { (arg0: Swift.UnsafeMutableRawPointer) in
                        let _arg0: any dependency._ExportedKotlinPackages_datetime_DateTimeFormatBuilder_WithDate = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: arg0) as! any dependency._ExportedKotlinPackages_datetime_DateTimeFormatBuilder_WithDate
                        let _result = originalBlock(_arg0)
                        return { _result; return true }()
                    }
                }())) as! any ExportedKotlinPackages.datetime.DateTimeFormat
            }
        }
        public init() {
            let __kt = datetime_LocalDate_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { datetime_LocalDate_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
}
