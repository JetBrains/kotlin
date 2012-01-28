package org.jetbrains.k2js;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author Pavel Talanov
 */
public interface Config {

    @NotNull
    Project getProject();
}
