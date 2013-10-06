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

    static Object returnNull() {
        return null;
    }

    static String returnString() {
        return "str";
    }

    static Object returnStringAsObject() {
        return "str";
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

    static int loop() {
        int i = 0;
        while (i < 10) i++;
        return i;
    }

    static int loopWithBreak() {
        int i = 0;
        while (true) {
            if (i > 10) break;
            i++;
        }
        return i;
    }

    static int loopWithReturn() {
        int i = 0;
        while (true) {
            if (i > 10) return i;
            i++;
        }
    }

    static int testSimpleFinally() {
        int i = 5;
        try {
            return i;
        }
        finally {
            i = 3;
        }
    }

    static int testSimpleFinallyWithReturn() {
        int i = 5;
        try {
            return i;
        }
        finally {
            return 3;
        }
    }

    static int testSimpleFinallyWithContinueInLoop() {
        int i = 5;
        while (true) {
            try {
                if (i % 2 == 0) continue;
                if (i > 10) return i;
            }
            finally {
                i++;
            }
        }
    }

    static int testSimpleFinallyWithBreakInLoop() {
        int i = 5;
        while (true) {
            try {
                if (i % 2 == 0) break;
            }
            finally {
                i++;
            }
        }
        return i;
    }

    static Object testCall() {
        return Integer.valueOf(1);
    }

    static Object testCallWithObject() {
        return String.valueOf("str");
    }

    static Object testGetStaticField() {
        return String.CASE_INSENSITIVE_ORDER;
    }

    static int FIELD = 0;

    static int testPutStaticField() {
        FIELD = 5;
        int f1 = FIELD;
        FIELD = 6;
        int f2 = FIELD;
        return f2 + f1;
    }

}
