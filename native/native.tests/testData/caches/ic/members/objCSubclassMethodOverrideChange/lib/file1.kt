package test

import objclib.NSObject

class KotlinSubclassOfNSObject : NSObject() {
    override fun description(): String? = "KotlinSubclassOfNSObject"
}
