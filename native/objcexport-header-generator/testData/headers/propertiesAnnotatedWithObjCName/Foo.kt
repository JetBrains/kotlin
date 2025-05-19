val Int.prop: Int
    get() = error("")

@ObjCName("objCName")
val Int.propWithObjCName: Int
    get() = error("")

@ObjCName("objCNameWithSwiftName", "swiftNameWithObjCName")
val Int.propWithObjCNameAndSwiftName: Int
    get() = error("")