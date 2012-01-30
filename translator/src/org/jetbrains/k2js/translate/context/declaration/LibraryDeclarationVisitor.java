package org.jetbrains.k2js.translate.context.declaration;

import org.jetbrains.annotations.NotNull;

import static org.jetbrains.k2js.translate.context.declaration.AnnotationsUtils.LIBRARY_CLASS_ANNOTATION_FQNAME;
import static org.jetbrains.k2js.translate.context.declaration.AnnotationsUtils.LIBRARY_FUNCTION_ANNOTATION_FQNAME;

/**
 * @author Pavel Talanov
 */
public final class LibraryDeclarationVisitor extends AnnotatedDeclarationVisitor {

    /*package*/ LibraryDeclarationVisitor(@NotNull Declarations nativeDeclarations) {
        super(nativeDeclarations, LIBRARY_CLASS_ANNOTATION_FQNAME, LIBRARY_FUNCTION_ANNOTATION_FQNAME);
    }
}
