// MODULE: cinterop
// FILE: lib.def
language = Objective-C
package = imported_from_cinterop.lib
---
@interface Base
-(instancetype) init;
-(void) overriddenFunction;
-(void) nonOverriddenFunction;
@property int overriddenProperty;
@property int nonOverriddenProperty;
@end

// MODULE: main(cinterop)
// FILE: main.kt
package imported_from_cinterop.main

import imported_from_cinterop.lib.Base

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class Derived : Base() {
    override fun overriddenFunction() = Unit
    //override fun nonOverriddenFunction() = Unit

    override fun overriddenProperty(): Int = 42
    //override fun nonOverriddenProperty(): Int = 42

    override fun setOverriddenProperty(overriddenProperty: Int) = Unit
    //override fun setNonOverriddenProperty(nonOverriddenProperty: Int) = Unit
}
