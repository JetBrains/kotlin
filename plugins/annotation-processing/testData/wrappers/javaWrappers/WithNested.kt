// FQNAME: WithNested

// FILE: WithNested.java
class WithNested {
    void myClassFun() {}
    
    static class NestedClass {
        void nestedClassFun() {}
    }
    
    class InnerClass {
        void innerClassFun() {}
        
        class InnerInnerClass {
            void innerInnerClassFun() {}
        }
    }
    
    interface NestedInterface {
        void nestedInterfaceFun() {}
    }
}

// FILE: Anno.kt
annotation class Anno