import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@interface Anon {
    String[] stringArray();

    int[] intArray();

    // string
    String string();
}

@Anon(string = "a", stringArray = { "a", "b" }, intArray = { 1, 2 })
@Target({ElementType.CONSTRUCTOR, ElementType.FIELD})
@interface I {
}
