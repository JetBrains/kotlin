// IGNORE_BACKEND_K1: ANY
// WITH_GUAVA
// FULL_JDK
// WITH_STDLIB

// FILE: X.java

import lombok.Data;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class X {
    @Singular private com.google.common.collect.ImmutableList<String> xStrings;
}

// FILE: Y.java

import lombok.Data;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class Y extends X {
    @Singular("singleYMap") private com.google.common.collect.ImmutableMap<String, Integer> yMap;
}

// FILE: Z.java

import lombok.Data;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class Z extends Y {
    @Singular private com.google.common.collect.ImmutableTable<String, String, String> zValues;
}

// FILE: test.kt

import com.google.common.collect.ImmutableTable
import com.google.common.collect.HashBasedTable

fun box(): String {
    val c = Z.builder()
        .xString("wrong")
        .clearXStrings()
        .xString("hello")
        .xStrings(listOf("world", "!"))
        .singleYMap("1", 1)
        .yMap(mapOf("2" to 2, "3" to 3))
        .zValue("1", "a", "hello")
        .zValues(ImmutableTable.of("2", "b", "world"))
        .build();

    val expectedZValues = HashBasedTable.create<String, String, String>().apply {
        put("1", "a", "hello")
        put("2", "b", "world")
    }

    return if (c.xStrings == listOf("hello", "world", "!") &&
        c.yMap == mapOf("1" to 1, "2" to 2, "3" to 3) &&
        c.zValues == expectedZValues
    ) {
        "OK"
    } else {
        "Error: $c"
    }
}