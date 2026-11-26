// Review of the PR around KT-91284: an explicit (non-synthetic) no-args Java constructor must never be hidden.
// Lombok keeps it as declared (checked with `javac` + Lombok 1.18.48 + `javap`):
//   public AllArgsWithExplicitNoArgs(); public AllArgsWithExplicitNoArgs(java.lang.String);
// Before commit "Lombok: hide explicit Java constructors if hidden by static factory" only
// `KtFakeSourceElementKind.ImplicitConstructor` constructors could be hidden by
// `hasDefaultConstructorHiddenByConstructorWithArgs`; now explicit ones are made private as well.

// FILE: AllArgsWithExplicitNoArgs.java
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AllArgsWithExplicitNoArgs {
    public String field;

    public AllArgsWithExplicitNoArgs() {
        field = "explicit";
    }

    static void javaUsage() {
        new AllArgsWithExplicitNoArgs();
        new AllArgsWithExplicitNoArgs("generated");
    }
}

// FILE: RequiredArgsWithExplicitNoArgs.java
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RequiredArgsWithExplicitNoArgs {
    public final String field;

    public RequiredArgsWithExplicitNoArgs() {
        field = "explicit";
    }

    static void javaUsage() {
        new RequiredArgsWithExplicitNoArgs();
        new RequiredArgsWithExplicitNoArgs("generated");
    }
}

// FILE: SuperBuilderWithExplicitNoArgs.java
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class SuperBuilderWithExplicitNoArgs {
    public String field;

    public SuperBuilderWithExplicitNoArgs() {
        field = "explicit";
    }

    static void javaUsage() {
        new SuperBuilderWithExplicitNoArgs();
    }
}

// FILE: test.kt
class SubAllArgs : AllArgsWithExplicitNoArgs()

fun box(): String {
    AllArgsWithExplicitNoArgs.javaUsage()
    RequiredArgsWithExplicitNoArgs.javaUsage()
    SuperBuilderWithExplicitNoArgs.javaUsage()

    if (AllArgsWithExplicitNoArgs().field != "explicit") return "Fail 1"
    if (AllArgsWithExplicitNoArgs("generated").field != "generated") return "Fail 2"
    if (RequiredArgsWithExplicitNoArgs().field != "explicit") return "Fail 3"
    if (RequiredArgsWithExplicitNoArgs("generated").field != "generated") return "Fail 4"
    if (SuperBuilderWithExplicitNoArgs().field != "explicit") return "Fail 5"
    if (SubAllArgs().field != "explicit") return "Fail 6"
    return "OK"
}
