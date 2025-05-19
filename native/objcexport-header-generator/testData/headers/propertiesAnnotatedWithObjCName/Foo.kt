val Int.prop: Int
    get() = error("")

@ObjCName("objCName")
val Int.propWithObjCName: Int
    get() = error("")

@ObjCName("objCNameWithSwiftName", "swiftNameWithObjCName")
val Int.propWithObjCNameAndSwiftName: Int
    get() = error("")

class Foo {
    val prop: Int = 42

    @ObjCName("objCName")
    val propWithObjCName: Int = 42

    @ObjCName("objCNameWithSwiftName", "swiftNameWithObjCName")
    val propWithObjCNameAndSwiftName: Int = 42
}