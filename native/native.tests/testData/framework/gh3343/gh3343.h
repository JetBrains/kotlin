#import <Foundation/Foundation.h>

NSString* foo1(int x) {
  return [NSString stringWithFormat:@"%d", x];
}

NSArray* foo2(int x) {
  NSValue* xx = @(x);
  NSString* s = @"zzz";
  return [NSArray arrayWithObjects: xx, s, nil];
}