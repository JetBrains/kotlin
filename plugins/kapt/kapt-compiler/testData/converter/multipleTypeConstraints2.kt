interface ReaderWriter<out T, in W> {
    fun read(): T
    fun write(value: W)
}

class Repository<
    T,
    out R,
    in W
>
where
    T : CharSequence,
    R : ReaderWriter<T, T>,
    T : Comparable<T>,
{
}
