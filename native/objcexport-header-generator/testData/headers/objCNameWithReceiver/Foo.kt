interface Foo {
    fun @receiver:ObjCName(swiftName = "ObjCName") Int.barWithSwiftName() = Unit
    fun @receiver:ObjCName("ObjCName") Int.barWithObjCName() = Unit
    fun @receiver:ObjCName("ObjCName", "SwiftName") Int.barWithObjCNameAndSwiftName() = Unit

    ObjCName("MethodObjCName")
    fun @receiver:ObjCName("ReceiverObjCName") Int.annotatedMethodAndReceiver() = Unit
}