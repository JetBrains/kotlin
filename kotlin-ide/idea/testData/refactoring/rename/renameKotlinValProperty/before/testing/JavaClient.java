package testing;

import testing.rename.*;

class JavaClient {
    public void foo(AP ap, DP dp) {
        ap.getFirst();
        new BP().getFirst();
        new CP().getFirst();

        dp.getFirst();
        new EP().getFirst();
        new FP().getFirst();
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