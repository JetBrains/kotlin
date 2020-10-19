/**
 *
 */
class Foo

class Bar1

<warning descr="SSR">//
class Bar2</warning>

<warning descr="SSR">/**/
class Bar3</warning>

class Bar4 {

    /**/
    fun f1(): Int = 1

    /**
     *
     */
    fun f2(): Int = 1

}