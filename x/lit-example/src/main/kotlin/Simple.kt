import kotlinx.browser.window
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
    var name = "Somebody"

    @Property()
    var count = 0

    override fun render(): Any {
        println("!render")
        window.setTimeout({
            name = "Lit"
        }, 3000)

        return html("""
            <p>Hello, $name! <div> AAA $name <div></p>
            <button @click=${::click}>click count: ${count}</button>
            """) // TODO(lit) can't use `.trimIndent()`
    }

    fun click() {
        println("!click")
        count++;
    }

    companion object {
        @JsStatic
        val styles = css("p { color: blue }")

        // SimpleGreeting.styles = css("p { color: blue }")
    }
}
