#ifndef _HEADER1_
#define _HEADER1_

@interface MyClass
@end

@interface MyClass(IncludeCategory)
@end

@interface MyClass(IncludeCategory2)
@end

@interface SkipClass
@end

@interface SkipClass(IncludeCategory)
@end

#endif
