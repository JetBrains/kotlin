// ERROR: Class 'A' must be declared abstract or implement abstract member public abstract fun run(): Unit defined in java.lang.Runnable
package foo

// we use package 'foo'

// imports:
import java.util.ArrayList // we need ArrayList

// let's declare a class:
internal class A /* just a sample name*/ : Runnable /* let's implement Runnable */ {
    fun foo/* again a sample name */(p: Int /* parameter p */, c: Char /* parameter c */) {
        // let's print something:
        println("1") // print 1
        println("2") // print 2

        println("3") // print 3

        // end of printing

        if (p > 0) { // do this only when p > 0
            // we print 4 and return
            println("3")
            return  // do not continue
        }

        // some code to be added
    }
} // end of class A