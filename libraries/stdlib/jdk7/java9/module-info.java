module kotlin.stdlib.jdk7 {
    requires transitive kotlin.stdlib;

    exports kotlin.jdk7;

    exports kotlin.internal.jdk7 to kotlin.stdlib.jdk8;
    opens kotlin.internal.jdk7 to kotlin.stdlib;
}
