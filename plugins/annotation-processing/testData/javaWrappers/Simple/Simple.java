// Simple

@KotlinAnnotation(a = "A", b = 5)
public abstract class Simple {
    final String field = "A";
    
    abstract void voidMethod();
    
    static {
        System.out.println("A");
    }
    
    {
        System.out.println("b");
    }
    
    protected String strMethod(int param) {
        return "A";
    }
}