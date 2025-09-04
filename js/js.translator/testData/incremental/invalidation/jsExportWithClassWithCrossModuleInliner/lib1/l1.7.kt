@JsName("baz") @JsExport inline fun foo() = 77
@JsName("gaz99") @JsExport class gaz(val foo: Int) {
    companion object {
        operator fun invoke(): Int = 99
    }
}
