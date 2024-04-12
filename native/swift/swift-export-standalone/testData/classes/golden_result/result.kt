import kotlin.native.internal.ExportedBridge

@ExportedBridge("why_we_need_module_names_bar")
public fun why_we_need_module_names_bar(): Int {
    val _result = why_we_need_module_names.bar()
    return _result
}

