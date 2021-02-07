package test

class Outer() {
    open inner class Inner1()
    
    inner class Inner2() : Inner1()
}
