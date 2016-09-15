// FQNAME: MyClass

class MyClass {
    companion object {
        val CONST = 2
    }
    
    fun myClassFun() {}
    
    class NestedClass {
        companion object A {
            val CONSTA = 3
        }
        
        fun nestedClassFun() {}
        
        class NestedNestedClass {
            fun nestedNestedClassFun() {}
        }
    }
    
    inner class InnerClass {
        fun innerClassFun() {}
    }
    
    interface InnerInterface {
        fun innerInterfaceFun()
        fun innerInterfaceFunWithImpl() = 5
    }
}