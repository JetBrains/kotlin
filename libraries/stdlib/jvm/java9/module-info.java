module kotlin.stdlib {
    exports kotlin;
    exports kotlin.annotation;
    exports kotlin.collections;
    exports kotlin.collections.unsigned;
    exports kotlin.comparisons;
    exports kotlin.concurrent;
    exports kotlin.contracts;
    exports kotlin.coroutines;
    exports kotlin.coroutines.intrinsics;
    exports kotlin.coroutines.jvm.internal;
    exports kotlin.io;
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

    // do not export experimental coroutines packages
    //exports kotlin.coroutines.experimental;
    //exports kotlin.coroutines.experimental.intrinsics;
    //exports kotlin.coroutines.experimental.jvm.internal;
    //exports kotlin.coroutines.experimental.migration;

    exports kotlin.experimental;

    exports kotlin.internal;
    exports kotlin.jvm.internal;
    exports kotlin.jvm.internal.markers;

    // Open packages with .kotlin_builtins files to kotlin-reflect, to allow reflection to load built-in declarations there
    opens kotlin to kotlin.reflect;
    opens kotlin.annotation to kotlin.reflect;
    opens kotlin.collections to kotlin.reflect;
    opens kotlin.coroutines to kotlin.reflect;
    opens kotlin.internal to kotlin.reflect;
    opens kotlin.ranges to kotlin.reflect;
    opens kotlin.reflect to kotlin.reflect;
}
