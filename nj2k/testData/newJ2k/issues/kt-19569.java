public class TestJavaBoxedPrimitives {
    public Object[] foo(Boolean x1, Byte x2, Short x3, Integer x4,
            Long x5, Float x6, Double x7, Character x8) {
        return new Object[]{x1, x2, x3, x4, x5, x6, x7, x8};
    }
}