package org.jetbrains.jet.j2k.visitors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.Converter;

/**
 * @author abreslav
 */
public interface J2KVisitor {
    @NotNull
    Converter getConverter();
}
