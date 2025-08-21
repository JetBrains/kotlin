package test;

public class NumberManager {

    static final String CONST = "STRING_CONST";
    static final int INT_CONST = 123 + 123;
    static int NOT_A_CONST = 1000;

    <T extends NumberHolder> T[] getAllHolders() {
        return null;
    }

    private MyEnum getMyEnum() {
        return null;
    }
}