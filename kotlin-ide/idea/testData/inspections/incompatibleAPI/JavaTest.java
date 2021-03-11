package problem.api;

import lib.LibClass;
import lib.LibMethods;
import lib.LibSuper;
import lib.LibConstructor;

public class JavaTest {
    void test() {
        new LibClass();
        LibMethods.staticMethod();
    }

    static void overloads(LibMethods lib) {
        lib.overload1(12);
        lib.overload1("Some");

        lib.overload2(12);
        lib.overload2("Some");

        //noinspection IncompatibleAPI
        lib.overload2(13);
    }

    public static class Extends extends LibClass {
    }

    public class Subclass extends LibSuper {
        @Override
        public void test(String str) {
        }
    }

    public class SubclassSuppress extends LibSuper {
        @SuppressWarnings("IncompatibleAPI")
        @Override
        public void test(String str) {
        }
    }

    public static constructor() {
        new LibConstructor(null, "some");
        new LibConstructor(null);
    }
}
