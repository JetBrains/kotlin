package org.jetbrains.eval4j.test;

class TestData {
    static void returnVoid() {
    }

    static boolean returnBoolean() {
        return true;
    }

    static byte returnByte() {
        return 2;
    }

    static short returnShort() {
        return 2;
    }

    static char returnChar() {
        return '2';
    }

    static int returnInt() {
        return 2;
    }

    static long returnLong() {
        return 2;
    }

    static float returnFloat() {
        return 2.0f;
    }

    static double returnDouble() {
        return 2.0d;
    }

    static int variable() {
        int i = 153;
        return i;
    }

    static int unaryMinus() {
        int i = 153;
        return -i;
    }

    static int ifThen() {
        boolean a = true;
        if (a)
            return 2;
        return 1;
    }

    static int ifElse() {
        boolean a = false;
        if (a) {
            return 2;
        }
        else {
            return 1;
        }
    }
}
