fun foo(
    param: Int,
    @ObjCName("objCName") paramWithObjCName: Int,
    @ObjCName("objCNameWithSwiftName", "swiftNameWithObjCName") paramWithObjCNameAndSwiftName: Int
) = Unit