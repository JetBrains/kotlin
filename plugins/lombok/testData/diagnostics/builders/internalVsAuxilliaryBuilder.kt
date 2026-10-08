// ISSUE: KT-83329
// USE_PSI_JAVA_FACADE: false
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

// Supported with java-direct only: the PSI Java facade reports MISSING_DEPENDENCY_CLASS for these calls
fun baz() {
    val builderArray = TestJava.builderArray()
    val builder = Other.builder(1)
    val qualifiedBuilder = Other.qualifiedBuilder(1)
    val fullyQualifiedBuilder = Other.fullyQualifiedBuilder(1)
}
