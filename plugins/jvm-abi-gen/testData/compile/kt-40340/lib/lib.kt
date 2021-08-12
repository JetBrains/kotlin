package lib;

import java.util.function.Supplier

fun withSupplier(s: Supplier<String>): String {
    return s.get()
}

inline fun inlineFunctionTakingSam(noinline stringSupplierFunction: () -> String): String {
    return withSupplier(stringSupplierFunction)
}
