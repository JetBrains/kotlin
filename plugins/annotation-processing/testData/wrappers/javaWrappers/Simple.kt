// FQNAME: Simple

// FILE: Simple.java
@interface Anno {
    String[] numbers();
}

@interface AnnoValue {
    String[] value();
}

@KotlinAnnotation(a = "A", b = 5)
@Anno(numbers = { "five", "six" })
@AnnoValue({ "five", "six" })
public abstract class Simple {
    @Anno(numbers = "seven")
    @AnnoValue("seven")
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

// FILE: KotlinAnnotation.kt
annotation class KotlinAnnotation(val a: String, val b: Int)