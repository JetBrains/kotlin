@SuppressWarnings("module") // suppress warnings about opening packages to not-yet-built kotlin-reflect
module kotlin.stdlib {
    exports kotlin;
    exports kotlin.annotation;
    exports kotlin.collections;
    exports kotlin.collections.unsigned;
    exports kotlin.comparisons;
    exports kotlin.concurrent;
    exports kotlin.contracts;
    exports kotlin.coroutines;
    exports kotlin.coroutines.cancellation;
    exports kotlin.coroutines.intrinsics;
    exports kotlin.coroutines.jvm.internal;
    exports kotlin.enums;
    exports kotlin.io;
    exports kotlin.io.encoding;
    exports kotlin.jvm;
    exports kotlin.jvm.functions;
    exports kotlin.math;
    exports kotlin.properties;
    exports kotlin.random;
    exports kotlin.ranges;
    exports kotlin.reflect;
    exports kotlin.sequences;
    exports kotlin.system;
    exports kotlin.text;
    exports kotlin.time;

    exports kotlin.experimental;

    exports kotlin.internal;
    exports kotlin.jvm.internal;
    exports kotlin.jvm.internal.markers;

    // JDK 7
    exports kotlin.jdk7;
    exports kotlin.io.path;

    // JDK 8
    exports kotlin.jvm.optionals;
    exports kotlin.jvm.jdk8;
    exports kotlin.collections.jdk8;
    exports kotlin.streams.jdk8;
    exports kotlin.text.jdk8;
    exports kotlin.time.jdk8;

    // Open packages with .kotlin_builtins files to kotlin-reflect, to allow reflection to load built-in declarations from there
    opens kotlin to kotlin.reflect;
    opens kotlin.annotation to kotlin.reflect;
    opens kotlin.collections to kotlin.reflect;
    opens kotlin.coroutines to kotlin.reflect;
    opens kotlin.internal to kotlin.reflect;
    opens kotlin.ranges to kotlin.reflect;
    opens kotlin.reflect to kotlin.reflect;
}
