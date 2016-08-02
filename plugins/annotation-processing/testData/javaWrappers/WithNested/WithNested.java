// WithNested

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