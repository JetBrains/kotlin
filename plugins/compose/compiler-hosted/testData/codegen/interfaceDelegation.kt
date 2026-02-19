// DUMP_IR

// MODULE: iface
// MODULE_KIND: LibraryBinary
// FILE: com/example/iface/MyInterface.kt
package com.example.iface

interface MyInterface {
    fun bar()
    fun foo() = "foo"
}

// MODULE: myModule(iface)
// FILE: com/example/myModule/MyInterfaceWrapper.kt
package com.example.myModule

import com.example.iface.MyInterface

@JvmInline
value class MyInterfaceWrapper(val myInterface: MyInterface) : MyInterface by myInterface

// MODULE: main(myModule, iface)
// FILE: main.kt
import com.example.iface.MyInterface
import com.example.myModule.MyInterfaceWrapper

fun main() {
    val my = MyInterfaceWrapper(object : MyInterface {
        override fun bar() {
            // body
        }
    })
    println(my.foo()) // prints "foo"
}
