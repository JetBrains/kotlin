package org.jetbrains.k2js;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author Pavel Talanov
 */
public final class IDEAConfig implements Config {

    @NotNull
    private final Project project;

    public IDEAConfig(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    @Override
    public Project getProject() {
        return project;
    }
}
