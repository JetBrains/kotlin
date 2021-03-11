package a;

public class GenericClass1 {
    public static <T> void staticFun1(JFunction1<T> r) {}
    public static <T> void staticFun2(JFunction1<T> r1, JFunction1<T> r2) {}
    public static <T> void staticFunWithOtherParam(int r1, JFunction1<T> r2) {}

    public <T> void memberFun1(JFunction1<T> r) {}
    public <T> void memberFun2(JFunction1<T> r1, JFunction1<T> r2) {}
    public <T> void memberFunWithOtherParam(int r1, JFunction1<T> r2) {}
}