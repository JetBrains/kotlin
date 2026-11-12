// FILE: lib/Conn.java
package lib;

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public interface Conn {
    void setTypeMap(Map<String, Class<?>> map);

    Map<String, ?> getProperties();

    void afterEvaluate(groovy.Closure closure);

    @NotNull
    List<String> findElements(@NotNull String by);

    @Deprecated
    void download(long timeout);
}

// FILE: groovy/Closure.java
package groovy;

public class Closure<V> {}

// FILE: test.kt
package test

import lib.Conn

abstract class ConnectionSpy(private val connection: Conn) : Conn by connection
