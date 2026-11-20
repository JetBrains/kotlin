// WITH_STDLIB

import lombok.Builder

// `BUILDER_REQUIRES_EXPLICIT_RETURN_TYPE` is still reported here.
// In this particular case it can be suppressed and works just by chance
// (we shouldn't infer builderClassName, and function itself does nothing dangerous),
// but it's not recommended to do so.
@Suppress("BUILDER_REQUIRES_EXPLICIT_RETURN_TYPE")
class Product(val x: Int) {
    companion object {
        @Builder(builderClassName = "FactoryBuilder")
        fun create(x: Int) = Product(x)
    }

    @Builder(builderClassName = "MemberBuilder", builderMethodName = "memberBuilder")
    fun describe(prefix: String) = prefix + x
}

// Uses `build()` from an implicitly typed function declared before the builder's owner.
fun early() = Product.builder().x(40).build().memberBuilder().prefix("x = ").build()

fun box(): String {
    val product: Product = Product.builder().x(1).build()
    if (product.x != 1) return "Fail: ${product.x}"
    val description: String = early()
    return if (description == "x = 40") "OK" else "Fail: $description"
}
