package kotlin.concurrent

import java.util.concurrent.Executor

deprecated("This class does not have any advantages compared to standard Java data structures and is going to be removed")
public class FunctionalQueue<T> (
        private val input: FunctionalList<T> = FunctionalList.emptyList<T>(),
        private val output: FunctionalList<T> = FunctionalList.emptyList<T>()
) {

    public val size: Int
        get() = input.size + output.size

    public val empty: Boolean
        get() = size == 0

    public fun add(element: T): FunctionalQueue<T> = FunctionalQueue<T>(input add element, output)

    public fun addFirst(element: T): FunctionalQueue<T> = FunctionalQueue<T>(input, output add element)

    public fun removeFirst(): Pair<T, FunctionalQueue<T>> =
            if (output.empty) {
                if (input.empty)
                    throw java.util.NoSuchElementException()
                else
                    FunctionalQueue<T>(FunctionalList.emptyList<T>(), input.reversed()).removeFirst()
            }
            else {
                Pair(output.head, FunctionalQueue<T>(input, output.tail))
            }
}
