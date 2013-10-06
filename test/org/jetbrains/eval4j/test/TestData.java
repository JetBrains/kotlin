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

    static int simpleFinally() {
        int i = 5;
        try {
            return i;
        }
        finally {
            i = 3;
        }
    }

    static int simpleFinallyWithReturn() {
        int i = 5;
        try {
            return i;
        }
        finally {
            return 3;
        }
    }

    static int simpleFinallyWithContinueInLoop() {
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

    static int simpleFinallyWithBreakInLoop() {
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

    static Object call() {
        return Integer.valueOf(1);
    }

    static Object callWithObject() {
        return String.valueOf("str");
    }

    static Object getStaticField() {
        return String.CASE_INSENSITIVE_ORDER;
    }

    static int FIELD = 0;

    static int putStaticField() {
        FIELD = 5;
        int f1 = FIELD;
        FIELD = 6;
        int f2 = FIELD;
        return f2 + f1;
    }

    static class C {
        int y = 15;

        C(int y) {
            this.y = y;
        }

        C() {}

        int getY() {
            return y;
        }

        static C newC() {
            return new C();
        }

        static void throwException() {
            throw new RuntimeException();
        }
    }

    static int getInstanceField() {
        return C.newC().y;
    }

    static int putInstanceField() {
        C c = C.newC();
        c.y = 5;
        int f1 = c.y;
        c.y = 6;
        int f2 = c.y;
        return f1 + f2;
    }

    static int instanceMethod() {
        return C.newC().getY();
    }

    static int constructorCallNoArgs() {
        return new C().y;
    }

    static int constructorCallWithArgs() {
        return new C(10).y;
    }

    static class MyEx extends RuntimeException {
        final int x;

        MyEx(int x) {
            this.x = x;
        }
    }

    static int tryCatch() {
        try {
            throw new MyEx(10);
        }
        catch (MyEx e) {
            return e.x;
        }
    }

    static int tryWiderCatch() {
        int a = 10;
        try {
            if (a > 0) {
                throw new MyEx(10);
            }
        } catch (Exception e) {
            return ((MyEx) e).x;
        }
        return 2;
    }

    static int classCastException() {
        Object a = "";
        try {
            Integer s = (Integer) a;
        }
        catch (ClassCastException e) {
            return 1;
        }
        return 2;
    }

    static String classLiteral() {
        return String.class.toString();
    }

    static int callThrowingMethod() {
        try {
            C.throwException();
        }
        catch (RuntimeException e) {
            return 1;
        }
        return 0;
    }

    static int NPE() {
        try {
            Object x = null;
            x.toString();
        }
        catch (NullPointerException e) {
            return 1;
        }
        return 0;
    }
}
