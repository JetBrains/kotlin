//file
package demo;

class Test {
    static void bar(int a) {
        if (a < 0)
            throw new RuntimeException("a = " + a);
    }
}