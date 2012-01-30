package org.jetbrains.k2js.translate.context.declaration;

import com.google.common.collect.Sets;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.NamingScope;
import org.jetbrains.k2js.translate.utils.BindingUtils;

import java.util.List;
import java.util.Set;

/**
 * @author Pavel Talanov
 */
//TODO: decide to maybe not to use different declaration objects
//TODO: methods receiver too many parameters and also unneccessary information like rootScope is stored inside
public final class DeclarationFacade {

    @NotNull
    public static DeclarationFacade createFacade(@NotNull NamingScope scope) {
        return new DeclarationFacade(scope);
    }

    @NotNull
    private final Declarations jetLibraryDeclarations;

    @NotNull
    private final Declarations kotlinDeclarations;

    @NotNull
    private final Declarations nativeDeclarations;

    @NotNull
    private final Declarations jsLibraryDeclarations;

    @NotNull
    private final NamingScope rootScope;

    private DeclarationFacade(@NotNull NamingScope rootScope) {
        this.rootScope = rootScope;
        this.jetLibraryDeclarations = Declarations.newInstance();
        this.kotlinDeclarations = Declarations.newInstance();
        this.nativeDeclarations = Declarations.newInstance();
        this.jsLibraryDeclarations = Declarations.newInstance();
    }

    @NotNull
    public Declarations getJetLibraryDeclarations() {
        return jetLibraryDeclarations;
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
        return Sets.newHashSet(jetLibraryDeclarations, kotlinDeclarations, nativeDeclarations, jsLibraryDeclarations);
    }

    @NotNull
    public DeclarationFacade extractStandardLibrary(@NotNull JetStandardLibrary standardLibrary,
                                                    @NotNull JsNameRef standardLibraryObjectName) {
        KotlinDeclarationVisitor visitor = new KotlinDeclarationVisitor(jetLibraryDeclarations, false);
        for (DeclarationDescriptor descriptor :
                standardLibrary.getLibraryScope().getAllDescriptors()) {
            descriptor.accept(visitor, DeclarationContext.rootContext(rootScope, standardLibraryObjectName));
        }
        return this;
    }

    //TODO: decide if is public
    public void extractDeclarationsFromNamespace(@NotNull NamespaceDescriptor descriptor,
                                                 @NotNull Namer namer) {
        KotlinDeclarationVisitor kotlinDeclarationVisitor = new KotlinDeclarationVisitor(kotlinDeclarations, true);
        kotlinDeclarationVisitor.traverseNamespace
                (descriptor, DeclarationContext.rootContext(rootScope, null));
        NativeDeclarationVisitor nativeDeclarationVisitor = new NativeDeclarationVisitor(nativeDeclarations);
        nativeDeclarationVisitor.traverseNamespace
                (descriptor, DeclarationContext.rootContext(rootScope, null));
        LibraryDeclarationVisitor libraryDeclarationVisitor = new LibraryDeclarationVisitor(jsLibraryDeclarations);
        libraryDeclarationVisitor.traverseNamespace
                (descriptor, DeclarationContext.rootContext(rootScope, namer.kotlinObject()));
    }

    public void extractDeclarationsFromFiles(@NotNull List<JetFile> files,
                                             @NotNull BindingContext context,
                                             @NotNull Namer namer) {
        for (NamespaceDescriptor namespace : getAllNamespaces(files, context)) {
            extractDeclarationsFromNamespace(namespace, namer);
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
