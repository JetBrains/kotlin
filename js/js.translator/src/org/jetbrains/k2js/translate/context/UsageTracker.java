package org.jetbrains.k2js.translate.context;

import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;

public class UsageTracker {
    private final DeclarationDescriptor trackedDescriptor;
    private boolean used;

    public UsageTracker(DeclarationDescriptor trackedDescriptor) {
        this.trackedDescriptor = trackedDescriptor;
    }

    public boolean isUsed() {
        return used;
    }

    public void triggerUsed(DeclarationDescriptor descriptor) {
        if (trackedDescriptor == descriptor) {
            used = true;
        }
    }
}
