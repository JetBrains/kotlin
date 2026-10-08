@interface ExtensionCategoriesLocal (Extensions)
- (NSString *)fromFrameworkOnLocal __attribute__((swift_name("fromFrameworkOnLocal()")));
@end

@interface ExtensionCategoriesReceiver (Extensions)
- (NSString *)fromFramework __attribute__((swift_name("fromFramework()")));
- (NSString *)fromIncluded __attribute__((swift_name("fromIncluded()")));
@end

@interface ExtensionCategoriesReceiver (exportedAlphaBeta)
- (NSString *)fromAlphaBeta __attribute__((swift_name("fromAlphaBeta()")));
@end

@interface ExtensionCategoriesReceiver (exportedAnotherBuild)
- (NSString *)fromAnotherBuild __attribute__((swift_name("fromAnotherBuild()")));
@end
