@file:OptIn(
  InternalComposeApi::class,
)
package test

import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable


    import kotlin.reflect.KProperty

    class Foo {
        var unstableField: Int = 0
    }

    @Composable
    inline operator fun Foo.getValue(thisObj: Any?, property: KProperty<*>): Foo = this

    @Composable
    fun test() {
        val foo by Foo()
        println(foo)
    }
