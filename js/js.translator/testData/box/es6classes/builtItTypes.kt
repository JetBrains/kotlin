// ES6_MODE
// EXPECTED_REACHABLE_NODES: 1331

// FILE: main.kt

// ~Lit
open class LitElement

// user code
@JsDecorator
@JsName("customElement")
external annotation class CustomElement(val tagName: String)

@JsDecorator
@JsName("property")
@Suppress("WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER")
external annotation class Property(val options: PropertyDeclaration = PropertyDeclaration())

@Suppress("WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER")
//@JsObjectLiteral
external annotation class PropertyDeclaration(
    val attribute: Boolean = true,
//    val converter:
    val noAccessor: Boolean = true,
    val reflect: Boolean = true,
    val state: Boolean = true,
//    val type: TypeHint,
//    val hasChanged
)

@JsDecorator
@JsName("property")
@Suppress("WRONG_DEFAULT_VALUE_FOR_EXTERNAL_FUN_PARAMETER")
external annotation class Property3(
    @JsOptionsLiteralParameter val attribute: Boolean = true,
//    val converter:
    @JsOptionsLiteralParameter val noAccessor: Boolean = true,
    @JsOptionsLiteralParameter val reflect: Boolean = true,
    @JsOptionsLiteralParameter val state: Boolean = true,
//    val type: TypeHint,
//    val hasChanged
)


@JsTemplateStringTag
fun html(s: String) {}
// Array<String>, vararg others: Any?

@JsTemplateStringTag
fun css(s: String): String = s

@CustomElement("simple-greeting")
class SimpleGreeting : LitElement() {
//    @Property(PropertyDeclaration(
//        attribute = false
//    ))
    @Property3(
        attribute = false
    )
    val name = "Somebody"

    fun render() {
        return html("""
            <p>Hello, $name! $name</p>
            """)
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
    return (a) => {
        console.log("customElement lambda: ", arguments);
        return a
    }
}

function property() {
    console.log("property: ", arguments);
    return (a) => {
        console.log("property lambda: ", arguments);
        return a
    }
}
