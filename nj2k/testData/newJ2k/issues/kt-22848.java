package test;

import java.util.Random;

public class Test {
    public static void x() {
        int a, b;
        switch (new Random().nextInt()) {
            case 0:
                a = b = 1;
                break;
            default:
                a = b = -1;
        }
        System.out.println(a + b);
    }
}