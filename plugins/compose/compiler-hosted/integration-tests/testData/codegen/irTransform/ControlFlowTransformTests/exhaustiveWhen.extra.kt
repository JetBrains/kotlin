sealed interface Alpha<T> {
    data class A<T>(
        val unused: Unit = Unit,
    ) : Alpha<T>

    data class B<T>(
        val unused: Unit = Unit,
    ) : Alpha<T>
}
