package verification.kt17916jt;

import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class CallerT extends TestWatcher {
    void twatcherCall(TestWatcher testWatcher) {
    }

    @Override
    protected void skipped(AssumptionViolatedException e, Description description) {
        super.skipped(e, description);
    }
}
