public class A {
    default object {
        public fun main(args: Array<String>) {
            System.out.println(Void.TYPE)
            System.out.println(Integer.TYPE)
            System.out.println(java.lang.Double.TYPE)
            System.out.println(javaClass<IntArray>())
            System.out.println(javaClass<Array<Any>>())
            System.out.println(javaClass<Array<Array<Any>>>())
        }
    }
}

fun main(args: Array<String>) = A.main(args)