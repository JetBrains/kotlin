package org.jetbrains.k2js.translate.context.declaration;

import com.google.common.collect.Sets;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.k2js.translate.context.NamingScope;
import org.jetbrains.k2js.translate.utils.BindingUtils;

import java.util.List;
import java.util.Set;

/**
 * @author Pavel Talanov
 */
public final class DeclarationFacade {

    @NotNull
    public static DeclarationFacade createFacade(@NotNull NamingScope scope) {
        return new DeclarationFacade(scope);
    }

    @NotNull
    private final Declarations libraryDeclarations;

    @NotNull
    private final Declarations kotlinDeclarations;

    @NotNull
    private final Declarations nativeDeclarations;

    @NotNull
    private final NamingScope rootScope;

    private DeclarationFacade(@NotNull NamingScope rootScope) {
        this.rootScope = rootScope;
        this.libraryDeclarations = Declarations.newInstance();
        this.kotlinDeclarations = Declarations.newInstance();
        this.nativeDeclarations = Declarations.newInstance();
    }

    @NotNull
    public Declarations getLibraryDeclarations() {
        return libraryDeclarations;
    }

    @NotNull
    public Declarations getKotlinDeclarations() {
        return kotlinDeclarations;
    }

    @NotNull
    public Declarations getNativeDeclarations() {
        return nativeDeclarations;
    }

    @NotNull
    public Set<Declarations> getAllDeclarations() {
        return Sets.newHashSet(libraryDeclarations, kotlinDeclarations, nativeDeclarations);
    }

    @NotNull
    public DeclarationFacade extractStandardLibrary(@NotNull JetStandardLibrary standardLibrary,
                                                    @NotNull JsNameRef standardLibraryObjectName) {
        KotlinDeclarationVisitor visitor = new KotlinDeclarationVisitor(libraryDeclarations, false);
        for (DeclarationDescriptor descriptor :
                standardLibrary.getLibraryScope().getAllDescriptors()) {
            descriptor.accept(visitor, DeclarationContext.rootContext(rootScope, standardLibraryObjectName));
        }
        return this;
    }

    //TODO: decide if is public
    public void extractDeclarationsFromNamespace(@NotNull NamespaceDescriptor descriptor,
                                                 @Nullable JsNameRef namespaceQualifier) {
        KotlinDeclarationVisitor visitor = new KotlinDeclarationVisitor(kotlinDeclarations, true);
        visitor.traverseNamespace(descriptor, DeclarationContext.rootContext(rootScope, namespaceQualifier));
    }

    public void extractDeclarationsFromFiles(@NotNull List<JetFile> files, @NotNull BindingContext context) {
        for (NamespaceDescriptor namespace : getAllNamespaces(files, context)) {
            extractDeclarationsFromNamespace(namespace, null);
        }
    }

    //TODO: util method
    @NotNull
    private Set<NamespaceDescriptor> getAllNamespaces(@NotNull List<JetFile> files, @NotNull BindingContext context) {
        Set<NamespaceDescriptor> namespaces = Sets.newHashSet();
        for (JetFile file : files) {
            namespaces.add(BindingUtils.getNamespaceDescriptor(context, file));
        }
        return namespaces;
    }

}
