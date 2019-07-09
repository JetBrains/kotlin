package foo; // we use package 'foo'

// imports:
import java.util.ArrayList; // we need ArrayList

// let's declare a class:
class A /* just a sample name*/ implements Runnable /* let's implement Runnable */ {
    void foo /* again a sample name */(int p /* parameter p */, char c /* parameter c */) {
        // let's print something:
        System.out.println("1"); // print 1
        System.out.println("2"); // print 2

        System.out.println("3"); // print 3

        // end of printing

        if (p > 0) { // do this only when p > 0
            // we print 4 and return
            System.out.println("3");
            return; // do not continue
        }

        // some code to be added
    }
} // end of class A