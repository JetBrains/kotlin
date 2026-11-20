// WITH_STDLIB

import lombok.Builder

// `BUILDER_REQUIRES_EXPLICIT_RETURN_TYPE` is only about inferring the builder class name, which is spelled out here,
// so a builder is generated, and its `build()` takes the annotated function's implicit return type.
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
