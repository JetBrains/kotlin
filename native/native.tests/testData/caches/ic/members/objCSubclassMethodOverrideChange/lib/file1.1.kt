package test

import objclib.NSObject
import objclib.NSUInteger

class KotlinSubclassOfNSObject : NSObject() {
    override fun hash(): NSUInteger = 42u
}
