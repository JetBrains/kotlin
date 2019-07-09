//file
package demo;

import java.util.*;

class Test implements Iterable<String> {
    @Override
    public Iterator<String> iterator() {
        return null;
    }

    public Iterator<String> push(Iterator<String> i) {
        Iterator<String> j = i;
        return j;
    }
}

class FullTest implements java.lang.Iterable<String> {
    @Override
    public java.util.Iterator<String> iterator() {
        return null;
    }

    public java.util.Iterator<String> push(java.util.Iterator<String> i) {
        java.util.Iterator<String> j = i;
        return j;
    }
}