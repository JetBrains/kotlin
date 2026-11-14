// libobjc authenticates the pointers in the Objective-C classes of a framework. They are signed as Clang signs them:
// with the DA key, their address and a constant for the isa (27361), superclass (46507) and data (25080) pointers of
// class_t and for the method list of class_ro_t (49936), and with the IA key and their address for method
// implementations.

// CHECK-DAG: @"OBJC_CLASS_$_[[CLASS:[A-Za-z0-9_]+Exported]]" = global %_class_t { ptr ptrauth (ptr @"OBJC_METACLASS_$_[[CLASS]]", i32 2, i64 27361, ptr @"OBJC_CLASS_$_[[CLASS]]"), ptr ptrauth (ptr @"OBJC_CLASS_$_{{[A-Za-z0-9_]+}}Base", i32 2, i64 46507, ptr getelementptr inbounds{{( nuw)?}} (%_class_t, ptr @"OBJC_CLASS_$_[[CLASS]]", i32 0, i32 1)), ptr @_objc_empty_cache, ptr null, ptr ptrauth (ptr @"\01l_OBJC_CLASS_RO_$_[[CLASS]]", i32 2, i64 25080, ptr getelementptr inbounds{{( nuw)?}} (%_class_t, ptr @"OBJC_CLASS_$_[[CLASS]]", i32 0, i32 4)) }
// CHECK-DAG: @"OBJC_METACLASS_$_[[METACLASS:[A-Za-z0-9_]+Exported]]" = global %_class_t { ptr ptrauth (ptr @"OBJC_METACLASS_$_NSObject", i32 2, i64 27361, ptr @"OBJC_METACLASS_$_[[METACLASS]]"),
// An empty method list stays null.
// CHECK-DAG: @"\01l_OBJC_CLASS_RO_$_{{[A-Za-z0-9_]+}}Exported" = private global %_class_ro_t { i32 0, i32 8, i32 8, ptr null, ptr @OBJC_CLASS_NAME_{{[.0-9]*}}, ptr null,
open class Exported {
    open fun method(): Int = 1
}

// The selectors holder is the class with methods.
// CHECK-DAG: @"\01l_OBJC_$_INSTANCE_METHODS_[[HOLDER_METHODS:[A-Za-z0-9_]+KotlinSelectorsHolder]]" = {{.*}}[%_objc_method { ptr @OBJC_METH_VAR_NAME_, ptr @OBJC_METH_VAR_TYPE_{{[.0-9]*}}, ptr ptrauth (ptr @{{[0-9]+}}, i32 0, i64 0, ptr getelementptr inbounds{{( nuw)?}} (%_objc_method, ptr getelementptr inbounds{{( nuw)?}} ({{.*}}, ptr @"\01l_OBJC_$_INSTANCE_METHODS_[[HOLDER_METHODS]]", i32 0, i32 2), i32 0, i32 2)) }
// CHECK-DAG: @"\01l_OBJC_CLASS_RO_$_[[HOLDER_RO:[A-Za-z0-9_]+KotlinSelectorsHolder]]" = {{.*}}ptr ptrauth (ptr @"\01l_OBJC_$_INSTANCE_METHODS_[[HOLDER_RO]]", i32 2, i64 49936, ptr getelementptr inbounds{{( nuw)?}} (%_class_ro_t, ptr @"\01l_OBJC_CLASS_RO_$_[[HOLDER_RO]]", i32 0, i32 5))

// C function pointers that the runtime adds to classes with class_addMethod, or calls.
// CHECK-DAG: ptr ptrauth (ptr @"objc2kotlin_virtual_kfun:Exported#method(){}kotlin.Int", i32 0) }
// CHECK-DAG: @"ktypew:kotlin.Function0" = global %struct.WritableTypeInfo { %struct.TypeInfoObjCExportAddition { ptr ptrauth (ptr @convertFunction0, i32 0),
// CHECK-DAG: = internal global [{{[0-9]+}} x ptr] [ptr ptrauth (ptr @convertBlock0, i32 0),
