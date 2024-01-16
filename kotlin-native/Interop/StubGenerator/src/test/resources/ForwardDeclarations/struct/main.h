#include "dependency.h"
#include "imported.h"
#include "included.h"

struct MainUsed;
struct MainUnused;
struct DependencyAndMain;

void useDependency(struct DependencyUsed*);
void useImported(struct ImportedUsed*);
void useIncluded(struct IncludedUsed*);
void useMain(struct MainUsed*);
