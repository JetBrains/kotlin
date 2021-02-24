@SuppressWarnings("module") // suppress warnings about terminal digit and exporting packages to not-yet-built kotlin-stdlib-jdk8
module kotlin.stdlib.jdk7 {
    requires transitive kotlin.stdlib;

    exports kotlin.jdk7;
    exports kotlin.io.path;

    exports kotlin.internal.jdk7 to kotlin.stdlib.jdk8;
    opens kotlin.internal.jdk7 to kotlin.stdlib;
}
