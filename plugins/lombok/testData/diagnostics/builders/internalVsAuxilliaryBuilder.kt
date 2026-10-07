// ISSUE: KT-83329
// FIR_DUMP
// FILE: test/java/TestJava.java

package test.java;

import lombok.Builder;

@Builder(builderMethodName = "internalBuilder")
public class TestJava {
    int a;

    public static TestJavaBuilder builder(int a) {
        return internalBuilder().a(a);
    }

    public static TestJava.TestJavaBuilder qualifiedBuilder(int a) {
        return internalBuilder().a(a);
    }

    public static test.java.TestJava.TestJavaBuilder fullyQualifiedBuilder(int a) {
        return internalBuilder().a(a);
    }

    public static TestJavaBuilder[] builderArray() {
        return null;
    }

    public static class NestedClass {
        public static TestJava.TestJavaBuilder qualifiedBuilder(int a) {
            return null;
        }

        public static test.java.TestJava.TestJavaBuilder fullyQualifiedBuilder(int a) {
            return null;
        }
    }
}

// FILE: test/java/Owner.java

package test.java;

import lombok.Builder;

public class Owner {
    @Builder(builderMethodName = "internalBuilder")
    public static class TestJava {
        int a;

        public static TestJavaBuilder builder(int a) {
            return internalBuilder().a(a);
        }

        public static Owner.TestJava.TestJavaBuilder qualifiedBuilder(int a) {
            return internalBuilder().a(a);
        }

        public static test.java.Owner.TestJava.TestJavaBuilder fullyQualifiedBuilder(int a) {
            return internalBuilder().a(a);
        }
    }
}

// FILE: test/other/Other.java

package test.other;

import test.java.TestJava.TestJavaBuilder;
import test.java.TestJava;

public class Other {
    public static TestJavaBuilder builder(int a) {
        return null;
    }

    public static TestJava.TestJavaBuilder qualifiedBuilder(int a) {
        return null;
    }

    public static test.java.TestJava.TestJavaBuilder fullyQualifiedBuilder(int a) {
        return null;
    }
}

// FILE: test.kt

import test.java.*
import test.other.Other

fun foo() {
    val internalBuilder = TestJava.internalBuilder()
    val builder = TestJava.builder(1)
    val qualifiedBuilder = TestJava.qualifiedBuilder(1)
    val fullyQualifiedBuilder = TestJava.fullyQualifiedBuilder(1)
    val qualifiedBuilderFromNested = TestJava.NestedClass.qualifiedBuilder(1)
    val fullyQualifiedBuilderFromNested = TestJava.NestedClass.fullyQualifiedBuilder(1)
}

fun bar() {
    val internalBuilder = Owner.TestJava.internalBuilder()
    val builder = Owner.TestJava.builder(1)
    val qualifiedBuilder = Owner.TestJava.qualifiedBuilder(1)
    val fullyQualifiedBuilder = Owner.TestJava.fullyQualifiedBuilder(1)
}

// All errors should fade away when we switch to Java direct
fun unsupported() {
    val builderArray = TestJava.<!MISSING_DEPENDENCY_CLASS!>builderArray<!>()
    val builder = Other.<!MISSING_DEPENDENCY_CLASS!>builder<!>(1)
    val qualifiedBuilder = Other.<!MISSING_DEPENDENCY_CLASS!>qualifiedBuilder<!>(1)
    val fullyQualifiedBuilder = Other.<!MISSING_DEPENDENCY_CLASS!>fullyQualifiedBuilder<!>(1)
}
