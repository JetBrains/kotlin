package foo

import js.*
import java.util.*

class NoSuchElementException() : Exception() {}

/*
Filters given iterator
*/
inline fun <T> Iterator<T>.filter(f: (T)-> Boolean) : Iterator<T> = FilterIterator<T>(this, f)

/*
Adds filtered elements in to given container
*/
inline fun <T,U : Collection<in T>> Iterable<T>.filterTo(var container: U, filter: (T)->Boolean) : U {
   for(element in this) {
      if(filter(element))
        container.add(element)
   }
   return container
}

/*
Create iterator filtering given java.lang.Iterable
*/
/*
inline fun <T> Iterable<T>.filter(f: (T)->Boolean) : Iterator<T> = (iterator() as Iterator<T>).filter(f)
*/

private class FilterIterator<T>(val original: Iterator<T>, val filter: (T)-> Boolean) : Iterator<T> {
    var state = 0
    var nextElement: T? = null

    override val hasNext: Boolean
    get() {
      if (state == 1) {
          return true;
      }
      if (state == 2) {
          return false;
      }
      while(original.hasNext) {
         val candidate = original.next()
          if((filter)(candidate)) {
              nextElement = candidate
              state = 1;
              return true;
          }
      }
        state = 2;
        return false;
    }

    override fun next(): T {
            val res = nextElement as T
            nextElement = null
            state = 0
            return res
        }
}
