// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-91096

// FILE: AllArgsNone.java
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
// AccessLevel.NONE: Lombok generates no constructor, so `javac` keeps the default one
@AllArgsConstructor(access = AccessLevel.NONE)
public class AllArgsNone {
    String field;
}

// FILE: RequiredArgsNone.java
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
@RequiredArgsConstructor(access = AccessLevel.NONE)
public class RequiredArgsNone {
    @NonNull String field;
}

// FILE: DataAndAllArgs.java
import lombok.AllArgsConstructor;
import lombok.Data;
// @Data skips its required-args constructor in presence of @AllArgsConstructor: only DataAndAllArgs(String) exists
@Data
@AllArgsConstructor
public class DataAndAllArgs {
    private String field;
}

// FILE: BuilderAndData.java
import lombok.Builder;
import lombok.Data;
// @Data skips its required-args constructor in presence of @Builder: only BuilderAndData(String) exists
@Builder
@Data
public class BuilderAndData {
    private String field;
}

// FILE: ValueAndBuilder.java
import lombok.Builder;
import lombok.Value;
// @Value makes `age` final, so the all-args constructor generated per @Builder is ValueAndBuilder(String)
@Value
@Builder
public class ValueAndBuilder {
    String name;
    int age = 10;
}

// FILE: test.kt
fun test() {
    AllArgsNone()
    RequiredArgsNone()
    <!INVISIBLE_REFERENCE!>DataAndAllArgs<!>()
    <!INVISIBLE_REFERENCE!>BuilderAndData<!>()
    ValueAndBuilder("name")
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaType, stringLiteral */
