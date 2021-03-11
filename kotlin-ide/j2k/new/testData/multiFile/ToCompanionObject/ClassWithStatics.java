package test;

class ClassWithStatics {
    public void instanceMethod() {
    }

    public static void staticMethod(int p) {
    }

    public static final int staticField = 1;
    public static int staticNonFinalField = 1;

    protected static int ourValue = 0;

    public static int getValue() {
        return ourValue;
    }

    public static void setValue(int value) {
        ourValue = value;
    }
}