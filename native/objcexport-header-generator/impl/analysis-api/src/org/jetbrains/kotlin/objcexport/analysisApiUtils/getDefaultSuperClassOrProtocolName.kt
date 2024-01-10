package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportClassOrProtocolName
import org.jetbrains.kotlin.objcexport.KtObjCExportSession
import org.jetbrains.kotlin.objcexport.getObjCKotlinStdlibClassOrProtocolName

/**
 * Some entities like top level functions are wrapped into classes with Base super class.
 *
 * @interface FooKt : Base
 * + (NSString *)myTopLevelFunction __attribute__((swift_name("myTopLevelFunction()")));
 * @end
 */
context(KtObjCExportSession)
internal fun getDefaultSuperClassOrProtocolName(): ObjCExportClassOrProtocolName {
    return "Base".getObjCKotlinStdlibClassOrProtocolName()
}