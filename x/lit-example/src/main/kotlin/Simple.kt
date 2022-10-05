import lit.*

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun main() {
    console.log("Hello, ${greet()}")
}

fun greet() = "world"

@CustomElement("simple-greeting")
class SimpleGreeting : LitElement() {
    @Property()
    val name = "Somebody"

    fun render() {
        return html("<p>Hello, $name! $name</p>")
//        html(["<p>Hello, ","!</p>"], name, name)
    }

    companion object {
        @JsStatic
        val styles = css("p { color: blue }")

        // SimpleGreeting.styles = css("p { color: blue }")
    }
}
