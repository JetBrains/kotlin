@protocol ProtocolDef
@end

__attribute__((objc_runtime_name("ProtocolDef2")))
@protocol ProtocolDefWithRuntimeName
@end

// Protocol forward declarations are not included to the metadata,
// so here we just test that cinterop doesn't crash.
@protocol ProtocolDecl;

__attribute__((objc_runtime_name("ProtocolDecl2")))
@protocol ProtocolDeclWithRuntimeName;
