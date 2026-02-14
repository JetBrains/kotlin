import kotlin.native.ObjCEnum
import kotlin.native.ObjCName
import kotlin.experimental.ExperimentalObjCEnum
import kotlin.experimental.ExperimentalObjCName

@file:OptIn(ExperimentalObjCEnum::class)

@ObjCEnum
enum class Foo {
    ALPHA_BETA,
    ALPHA,
    COPY,
    @ObjCName("objCName1Renamed") OBJC_NAME_1_ORIGINAL,
    @ObjCName(name = "objcName2Renamed", swiftName = "objcName2Swift") OBJC_NAME_2_ORIGINAL,
    @ObjCEnum.EntryName(name="entryName1Renamed") ENTRY_NAME_1_ORIGINAL,
    @ObjCEnum.EntryName(name="entryName2Renamed", swiftName="entryName2Swift") ENTRY_NAME_2_SWIFT,
    @ObjCEnum.EntryName(name="combination1Renamed") @ObjCName(name="combination1Bad") COMBINATION_1,
    @ObjCEnum.EntryName(name="combination2Renamed") @ObjCName(name="combination2BadObjC", swiftName="combination2BadSwift") COMBINATION_2
}