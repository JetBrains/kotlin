// DUMP_IR

// MODULE: main
// FILE: main.kt
package com.example.myModule

class OtherModule {
    inline fun giveMeString() : String {
        return secret()
    }

    @PublishedApi
    internal fun secret() : String {
        return "what is up!!!!!!!"
    }
}
