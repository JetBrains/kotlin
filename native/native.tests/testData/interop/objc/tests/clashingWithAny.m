#import "clashingWithAny.h"

@implementation TestClashingWithAny1
-(NSString*)description {
    return @"description";
}

-(NSString*)toString {
    return @"toString";
}

-(NSString*)toString_ {
    return @"toString_";
}

-(NSUInteger)hash {
    return 1;
}

-(int)hashCode {
    return 31;
}

-(BOOL)equals:(id _Nullable)other {
    return YES;
}
@end

@implementation TestClashingWithAny2
-(NSString*)description {
    return @"description";
}

-(void)toString {
}

-(NSUInteger)hash {
    return 2;
}

-(void)hashCode {
}

-(void)equals:(int)p {
}
@end

@implementation TestClashingWithAny3
-(NSString*)description {
    return @"description";
}

-(NSString*)toString:(int)p {
    return [NSString stringWithFormat:@"%s:%d", "toString", p];
}

-(NSUInteger)hash {
    return 3;
}

-(int)hashCode:(int)p {
    return p + 1;
}

-(BOOL)equals {
    return YES;
}
@end