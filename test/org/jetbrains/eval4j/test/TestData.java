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

    static Class<?> arrayClass() {
        return int[].class;
    }

    static int arrayOfByte() {
        byte[] a = new byte[] {1, 2, 3};
        int sum = 0;
        for (int i : a) {
            sum += i;
        }
        return sum;
    }

    static int arrayOfShort() {
        short[] a = new short[] {1, 2, 3};
        int sum = 0;
        for (int i : a) {
            sum += i;
        }
        return sum;
    }

    static int arrayOfChar() {
        char[] a = new char[] {1, 2, 3};
        int sum = 0;
        for (int i : a) {
            sum += i;
        }
        return sum;
    }

    static int arrayOfInt() {
        int[] a = new int[] {1, 2, 3};
        int sum = 0;
        for (int i : a) {
            sum += i;
        }
        return sum;
    }

    static int arrayOfLong() {
        long[] a = new long[] {1, 2, 3};
        int sum = 0;
        for (long i : a) {
            sum += i;
        }
        return sum;
    }

    static float arrayOfFloat() {
        float[] a = new float[] {1, 2, 3};
        float sum = 0;
        for (float i : a) {
            sum += i;
        }
        return sum;
    }

    static double arrayOfDouble() {
        double[] a = new double[] {1, 2, 3};
        double sum = 0;
        for (double i : a) {
            sum += i;
        }
        return sum;
    }

    static String arrayOfString() {
        String[] a = new String[] {"1", "2", "3"};
        String sum = "";
        for (String i : a) {
            sum += i;
        }
        return sum;
    }

    static int arrayOfByte2() {
        byte[][] a = new byte[][] {{1}, {2}, {3}};
        int sum = 0;
        for (byte[] aa: a)
            for (int i : aa) {
                sum += i;
            }
        return sum;
    }

    static int arrayOfShort2() {
        short[][] a = new short[][] {{1}, {2}, {3}};
        int sum = 0;
        for (short[] aa: a)
            for (int i : aa) {
                sum += i;
            }
        return sum;
    }

    static int arrayOfChar2() {
        char[][] a = new char[][] {{1}, {2}, {3}};
        int sum = 0;
        for (char[] aa: a)
            for (int i : aa) {
                sum += i;
            }
        return sum;
    }

    static int arrayOfInt2() {
        int[][] a = new int[][] {{1}, {2}, {3}};
        int sum = 0;
        for (int[] aa: a)
            for (int i : aa) {
                sum += i;
            }
        return sum;
    }

    static int arrayOfLong2() {
        long[][] a = new long[][] {{1}, {2}, {3}};
        int sum = 0;
        for (long[] aa: a)
            for (long i : aa) {
                sum += i;
            }
        return sum;
    }

    static float arrayOfFloat2() {
        float[][] a = new float[][] {{1}, {2}, {3}};
        float sum = 0;
        for (float[] aa: a)
            for (float i : aa) {
                sum += i;
            }
        return sum;
    }

    static double arrayOfDouble2() {
        double[][] a = new double[][] {{1}, {2}, {3}};
        double sum = 0;
        for (double[] aa: a)
            for (double i : aa) {
                sum += i;
            }
        return sum;
    }

    static String arrayOfString2() {
        String[][] a = new String[][] {{"1"}, {"2"}, {"3"}};
        String sum = "";
        for (String[] aa: a)
            for (String i : aa) {
                sum += i;
            }
        return sum;
    }

    static String multiArrayOfInt() {
        int[][] a = new int[2][2];
        String s = "";
        for (int[] x : a)
            for (int y : x) {
                s += y;
            }
        return s;
    }

    static String multiArrayOfString() {
        String[][] a = new String[2][2];
        for (String[] x : a)
            for (int i = 0; i < x.length; i++) {
                x[i] = i + "";
            }
        String s = "";
        for (String[] x : a)
            for (String y : x) {
                s += y;
            }
        return s;
    }
}
