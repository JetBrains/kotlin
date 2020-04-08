package test

class NestedClasses {
    fun f() {
    }

    private class Nested {
        fun f() {
        }

        public class NN {
            fun f() {
            }
        }

        inner class NI {
            fun f() {
            }
        }
    }

    public inner class Inner {
        fun f() {
        }

        private inner class II {
            fun f() {
            }
        }
    }
}
