@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_ListExport_2
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinStdlib

extension ExportedKotlinPackages.list2.MyList where Self : KotlinRuntimeSupport._KotlinBridgeable {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.list2.MyList where Wrapped : ExportedKotlinPackages.list2._MyList {
}
extension ExportedKotlinPackages.list2 {
    public protocol MyList: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.List {
    }
    @objc(_MyList)
    package protocol _MyList: ExportedKotlinPackages.kotlin.collections._List {
    }
    public static func testStarList(
        l: any ExportedKotlinPackages.list2.MyList
    ) -> any ExportedKotlinPackages.list2.MyList {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: list2_testStarList__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList__(l.__externalRCRef())) as! any ExportedKotlinPackages.list2.MyList
    }
}
