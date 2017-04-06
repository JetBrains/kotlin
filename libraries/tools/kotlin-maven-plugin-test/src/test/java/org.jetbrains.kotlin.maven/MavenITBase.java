package org.jetbrains.kotlin.maven;

import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public abstract class MavenITBase {
    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            System.out.println("Running test: " + description.getDisplayName());
        }
    };
}
