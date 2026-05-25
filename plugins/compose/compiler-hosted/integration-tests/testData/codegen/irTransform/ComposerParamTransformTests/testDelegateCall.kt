@file:OptIn(
  InternalComposeApi::class,
)
package test

import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable


    import kotlin.reflect.KProperty

    class Foo
    @Composable
    operator fun Foo.getValue(thisObj: Any?, property: KProperty<*>): Foo = this

    class FooDelegate {
        @Composable
        operator fun getValue(thisObj: Any?, property: KProperty<*>): FooDelegate = this
    }

    class Bar {
        @get:Composable
        val foo by Foo()
    }

    @Composable
    fun test() {
        val foo by Foo()
        val fooDelegate by FooDelegate()
        val bar = Bar()
        println(foo)
        println(fooDelegate)
        println(bar.foo)
    }
