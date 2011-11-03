package org.jetbrains.k2js.translate;

/**
 * @author Talanov Pavel
 */
public enum ContextType {
    CLASS_DECLARATION {
        public DeclarationTranslator.State getDeclarationTranslatorState(DeclarationTranslator translator) {
            throw new UnsupportedOperationException("Not impelemented");
        }
    },
    FUNCTION_BODY {
        public DeclarationTranslator.State getDeclarationTranslatorState(DeclarationTranslator translator) {
            return translator.new FunctionVariableDeclaration();
        };
    },
    NAMESPACE_BODY {
        public DeclarationTranslator.State getDeclarationTranslatorState(DeclarationTranslator translator) {
            return translator.new NamespacePropertyDeclaration();
        };
    };

    public abstract DeclarationTranslator.State getDeclarationTranslatorState(DeclarationTranslator translator);
}
