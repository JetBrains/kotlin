// MODULE: main
// FILE: sample/Placeholder.kt
package sample

// FILE: sample/JavaClass.java
package sample;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JavaClass {
    public void none(String value) {}
    public void notNull(@NotNull String value) {}
    public void nullable(@Nullable String value) {}
    public static void staticNone(String value) {}
    public static void staticNotNull(@NotNull String value) {}
}

// The nullability annotations are compiled from the fixture itself, like the KDM analyzer's own test classpath.
// FILE: org/jetbrains/annotations/NotNull.java
package org.jetbrains.annotations;

public @interface NotNull {}

// FILE: org/jetbrains/annotations/Nullable.java
package org.jetbrains.annotations;

public @interface Nullable {}
