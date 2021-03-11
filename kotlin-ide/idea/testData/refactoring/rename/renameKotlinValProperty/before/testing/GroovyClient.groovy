package testing;

import testing.rename.*;

class GroovyClient {
    public void foo(AP ap, DP dp) {
        ap.first
        new BP().first
        new CP().first

        dp.first
        new EP().first
        new FP().first
    }

    public interface DP extends AP {
    }

    public static class EP implements DP {
        @Override
        public int getFirst() {
            return 3;
        }
    }

    public static class FP extends EP {
        @Override
        public int getFirst() {
            return 4;
        }
    }
}
