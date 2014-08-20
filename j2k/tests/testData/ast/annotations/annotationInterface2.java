@interface Anon {
    String s() default "a";
    String[] stringArray() default { "a", "b" };
    int[] intArray();
}

@Anon(intArray = {1, 2})
class A{ }
