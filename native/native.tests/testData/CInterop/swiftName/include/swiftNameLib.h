// A class renamed only through the in-header `swift_name` attribute (no API notes entry).
__attribute__((swift_name("SourceRenamedSwift")))
@interface SourceRenamed
@end

// A class renamed only through API notes (no in-header attribute).
@interface NotesRenamed
@end

// A class renamed by both: API notes must win (matching Clang).
__attribute__((swift_name("OverriddenHeaderSwift")))
@interface Overridden
@end

// A protocol renamed through API notes.
@protocol NotesProtocol
@end
