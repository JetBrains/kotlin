// Method override / inheritance interacting with `swift_name`.
//
// In Objective-C/Swift the Swift name of an overriding method is owned by the declaration that
// introduces the selector; overrides inherit it, and an explicit name on a derived-only override is
// dropped. In cinterop a subclass redeclaration that only differs from the inherited method by its
// `swift_name` must therefore be dropped rather than re-emitted as a duplicate member.

// A1: base explicit, child implicit override.
//     Both `a1` denote the same member; the subclass must not re-emit it.
@interface A1Base
- (void)a1 __attribute__((swift_name("a1_renamed()")));
@end
@interface A1Child : A1Base
- (void)a1;
@end

// A2: base implicit, child explicit.
//     The child's `swift_name` is dropped (Swift owns the name at the introducing base declaration);
//     the redeclaration must still be deduplicated away.
@interface A2Base
- (void)a2;
@end
@interface A2Child : A2Base
- (void)a2 __attribute__((swift_name("a2_renamed()")));
@end

// A3: both explicit, matching.
@interface A3Base
- (void)a3 __attribute__((swift_name("a3_renamed()")));
@end
@interface A3Child : A3Base
- (void)a3 __attribute__((swift_name("a3_renamed()")));
@end

// A4: 3-level; the middle class neutralizes with an implicit override.
@interface A4Base
- (void)a4 __attribute__((swift_name("a4_renamed()")));
@end
@interface A4Middle : A4Base
- (void)a4;
@end
@interface A4Grand : A4Middle
- (void)a4;
@end

// A5: skip-level (gap) override — A5Middle does not declare a5 at all.
@interface A5Base
- (void)a5 __attribute__((swift_name("a5_renamed()")));
@end
@interface A5Middle : A5Base
@end
@interface A5Grand : A5Middle
- (void)a5;
@end
