// ES6_MODE
// EXPECTED_REACHABLE_NODES: 1331

// FILE: main.kt

// to stdlib
annotation class JsDecorator
annotation class JsTemplateStringTag

// ~Lit
open class LitElement

// user code
@JsDecorator
@JsName("customElement")
external annotation class CustomElement(val tagName: String)

@JsDecorator
@JsName("property")
external annotation class Property(/*val options: PropertyDeclaration? = null*/)
annotation class JsStatic

@JsTemplateStringTag
fun html(s: String) {}
// Array<String>, vararg others: Any?

@JsTemplateStringTag
fun css(s: String): String = s

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

// SimpleGreeting.Companion = ...
fun box(): String {
    return "OK"
}

// FILE: mylit.js

function customElement() {
    console.log("customElement: ", arguments);
}

function property() {
    console.log("property: ", arguments);
}
