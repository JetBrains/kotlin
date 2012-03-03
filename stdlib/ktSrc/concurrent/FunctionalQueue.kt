package kotlin.concurrent

import java.util.concurrent.Executor
import jet.Iterator

class FunctionalQueue<T> (
    val input: FunctionalList<T> = FunctionalList.emptyList<T>(),
    val output: FunctionalList<T> = FunctionalList.emptyList<T>()) {

    val size : Int
        get() = input.size + output.size

    val empty : Boolean
        get() = size == 0

    fun add(element: T) = FunctionalQueue<T>(input add element, output)

    fun addFirst(element: T) = FunctionalQueue<T>(input, output add element)

    fun removeFirst() : #(T,FunctionalQueue<T>) =
        if(output.empty) {
            if(input.empty)
                throw java.util.NoSuchElementException()
            else
                FunctionalQueue<T>(FunctionalList.emptyList<T>(), input.reversed()).removeFirst()
        }
        else {
            #(output.head, FunctionalQueue<T>(input, output.tail))
        }
}
