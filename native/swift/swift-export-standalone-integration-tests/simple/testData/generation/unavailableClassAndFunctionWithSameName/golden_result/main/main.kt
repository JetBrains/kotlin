@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(com.github.jetbrains.swiftexport.se.Banner::class, "22ExportedKotlinPackages3comO6githubO9jetbrainsO11swiftexportO2seO4mainE6BannerC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("com_github_jetbrains_swiftexport_se_Banner_init_allocate")
public fun com_github_jetbrains_swiftexport_se_Banner_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<com.github.jetbrains.swiftexport.se.Banner>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("com_github_jetbrains_swiftexport_se_Banner_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__")
public fun com_github_jetbrains_swiftexport_se_Banner_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(__kt: kotlin.native.internal.NativePtr, text: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __text = interpretObjCPointer<kotlin.String>(text)
    kotlin.native.internal.initInstance(____kt, com.github.jetbrains.swiftexport.se.Banner(__text))
    return true
}

@ExportedBridge("com_github_jetbrains_swiftexport_se_Banner_text_get")
public fun com_github_jetbrains_swiftexport_se_Banner_text_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as com.github.jetbrains.swiftexport.se.Banner
    val _result = __self.text
    return _result.objcPtr()
}
