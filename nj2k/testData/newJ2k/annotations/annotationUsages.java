import javaApi.*;

@Anon1(value = {"a"}, stringArray = {"b"}, intArray = {1, 2}, string = "x")
@Anon2(value = "a", intValue = 1, charValue = 'a')
@Anon3(e = E.A, stringArray = {}, value = {"a", "b"})
@Anon4({"x", "y"})
@Anon5(1)
@Anon6({"x", "y"})
@Anon7({ String.class, StringBuilder.class })
@Anon8(classes = { String.class, StringBuilder.class })
class C {
    @Anon5(1) @Deprecated private int field1 = 0;

    @Anon5(1)
    private int field2 = 0;

    @Anon5(1) int field3 = 0;

    @Anon5(1)
    int field4 = 0;

    @Anon6({})
    void foo(@Deprecated int p1, @Deprecated @Anon5(2) char p2) {
        @Deprecated @Anon5(3) char c = 'a';
    }

    @Anon5(1) void bar(){}
}
