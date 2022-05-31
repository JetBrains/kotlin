package org.jetbrains.dokka.base.translators

// TODO [beresnev] remove this copy-paste and use the same method from stdlib instead after updating to 1.5
internal inline fun <T, R : Any> Iterable<T>.firstNotNullOfOrNull(transform: (T) -> R?): R? {
    for (element in this) {
        val result = transform(element)
        if (result != null) {
            return result
        }
    }
    return null
}
