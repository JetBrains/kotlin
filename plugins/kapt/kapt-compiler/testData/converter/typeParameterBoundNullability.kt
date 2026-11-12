class Box<T>

infix fun <T : Number?, S : T> Box<T>.mod(t: S): Int = 0

infix fun <T : Number, S : T> Box<T>.rem(t: S): Int = 0

interface Builder {
    infix fun <T : Number?, S : T> Box<T>.mod(t: S): Int = 0
}
