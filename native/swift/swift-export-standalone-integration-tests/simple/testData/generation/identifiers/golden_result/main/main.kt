@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("U30_0times_get")
public fun U30_0times_get(): kotlin.native.internal.NativePtr {
    val _result = `0`.`0times`
    return _result.objcPtr()
}

@ExportedBridge("U30____get")
public fun U30____get(): kotlin.native.internal.NativePtr {
    val _result = `0`.`__`
    return _result.objcPtr()
}

@ExportedBridge("U30___get")
public fun U30___get(): kotlin.native.internal.NativePtr {
    val _result = `0`.`_`
    return _result.objcPtr()
}

@ExportedBridge("U30_ascii_get")
public fun U30_ascii_get(): kotlin.native.internal.NativePtr {
    val _result = `0`.ascii
    return _result.objcPtr()
}

@ExportedBridge("U30_for_get")
public fun U30_for_get(): kotlin.native.internal.NativePtr {
    val _result = `0`.`for`
    return _result.objcPtr()
}

@ExportedBridge("U30_withU20space_get")
public fun U30_withU20space_get(): kotlin.native.internal.NativePtr {
    val _result = `0`.`with space`
    return _result.objcPtr()
}

@ExportedBridge("U30_U221E_get")
public fun U30_U221E_get(): kotlin.native.internal.NativePtr {
    val _result = `0`.`∞`
    return _result.objcPtr()
}

@ExportedBridge("U30_UD83EDD37_get")
public fun U30_UD83EDD37_get(): kotlin.native.internal.NativePtr {
    val _result = `0`.`🤷`
    return _result.objcPtr()
}

@ExportedBridge("U31_0times")
public fun U31_0times(): kotlin.native.internal.NativePtr {
    val _result = `1`.`0times`()
    return _result.objcPtr()
}

@ExportedBridge("U31__")
public fun U31__(): kotlin.native.internal.NativePtr {
    val _result = `1`.`_`()
    return _result.objcPtr()
}

@ExportedBridge("U31___")
public fun U31___(): kotlin.native.internal.NativePtr {
    val _result = `1`.`__`()
    return _result.objcPtr()
}

@ExportedBridge("U31_ascii")
public fun U31_ascii(): kotlin.native.internal.NativePtr {
    val _result = `1`.ascii()
    return _result.objcPtr()
}

@ExportedBridge("U31_for__TypesOfArguments__Swift_Int32_Swift_Int32_Swift_Int32__")
public fun U31_for__TypesOfArguments__Swift_Int32_Swift_Int32_Swift_Int32__(int: Int, `for`: Int, `for long`: Int): kotlin.native.internal.NativePtr {
    val __int = int
    val __for = `for`
    val `__for long` = `for long`
    val _result = `1`.`for`(__int, __for, `__for long`)
    return _result.objcPtr()
}

@ExportedBridge("U31_withU20space")
public fun U31_withU20space(): kotlin.native.internal.NativePtr {
    val _result = `1`.`with space`()
    return _result.objcPtr()
}

@ExportedBridge("U31_U221E")
public fun U31_U221E(): kotlin.native.internal.NativePtr {
    val _result = `1`.`∞`()
    return _result.objcPtr()
}

@ExportedBridge("U31_UD83EDD37")
public fun U31_UD83EDD37(): kotlin.native.internal.NativePtr {
    val _result = `1`.`🤷`()
    return _result.objcPtr()
}

