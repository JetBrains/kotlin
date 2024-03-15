#import "dependency.h"
#import "imported.h"
#import "included.h"


@class MainClassUsed;
@class MainClassUnused;
@class DependencyAndMainClass;

@protocol MainProtocolUsed;
@protocol MainProtocolUnused;
@protocol DependencyAndMainProtocol;

void useDependency(DependencyClassUsed*, id<DependencyProtocolUsed>);
void useImported(ImportedClassUsed*, id<ImportedProtocolUsed>);
void useIncluded(IncludedClassUsed*, id<IncludedProtocolUsed>);
void useMain(MainClassUsed*, id<MainProtocolUsed>);
