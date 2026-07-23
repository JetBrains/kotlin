package androidx.compose.compiler.plugins;

import com.google.protobuf.Internal;

/**
 * Simulates a Protobuf Edition 2023 large enum, which is generated as a Java CLASS
 * implementing com.google.protobuf.Internal.EnumLite instead of a Java ENUM.
 */
public final class LargeEnumClass implements Internal.EnumLite {
    private final int value;

    public LargeEnumClass(int value) {
        this.value = value;
    }

    @Override
    public int getNumber() {
        return value;
    }

    public static final LargeEnumClass ZERO = new LargeEnumClass(0);
    public static final LargeEnumClass ONE = new LargeEnumClass(1);
}
