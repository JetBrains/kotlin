package test;

import javaApi.*;

public class Test {
    private Listener listener = new Listener() {
        @Override
        public void onChange(int visibility) {
            val a = (visibility & 1)
        }
    }
}