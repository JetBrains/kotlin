/**
 * @deprecated This module is empty and therefore deprecated, please use <code>kotlin.stdlib</code> instead.
 */
@SuppressWarnings("module") // suppress warning about terminal digit
@Deprecated
module kotlin.stdlib.jdk8 {
    requires transitive kotlin.stdlib;
    requires kotlin.stdlib.jdk7;
}
