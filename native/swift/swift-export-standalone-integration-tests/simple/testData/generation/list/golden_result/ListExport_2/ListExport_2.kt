@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(list2.MyList::class, "_MyList")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("list2_testStarList__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList__")
public fun list2_testStarList__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList__(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as list2.MyList<kotlin.Any?>
    val _result = list2.testStarList(__l)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
