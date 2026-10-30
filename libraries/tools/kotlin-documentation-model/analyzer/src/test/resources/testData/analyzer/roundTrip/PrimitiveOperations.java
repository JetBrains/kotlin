// MODULE: main
// FILE: example/PrimitiveOperations.java
package example;

public interface PrimitiveOperations {
    boolean accept(boolean enabled, byte value, short count, char marker);

    Boolean acceptBoxed(Boolean enabled, Byte value, Short count, Character marker);

    double calculate(float value, double scale);

    Double calculateBoxed(Float value, Double scale);

    long count(int value, long total);

    Long countBoxed(Integer value, Long total);

    void reset();

    Void resetBoxed();
}
