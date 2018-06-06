module kotlin.reflect {
    requires transitive kotlin.stdlib;

    exports kotlin.reflect.full;
    exports kotlin.reflect.jvm;

    opens kotlin.reflect.jvm.internal to kotlin.stdlib;

    uses org.jetbrains.kotlin.builtins.BuiltInsLoader;
    uses org.jetbrains.kotlin.resolve.ExternalOverridabilityCondition;
    uses org.jetbrains.kotlin.util.ModuleVisibilityHelper;

    provides org.jetbrains.kotlin.builtins.BuiltInsLoader with org.jetbrains.kotlin.builtins.BuiltInsLoaderImpl;
    provides org.jetbrains.kotlin.resolve.ExternalOverridabilityCondition with
            org.jetbrains.kotlin.load.java.FieldOverridabilityCondition,
            org.jetbrains.kotlin.load.java.ErasedOverridabilityCondition,
            org.jetbrains.kotlin.load.java.JavaIncompatibilityRulesOverridabilityCondition;
}
