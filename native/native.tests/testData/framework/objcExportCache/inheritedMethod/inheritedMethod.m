#import <Foundation/Foundation.h>
#import <Kt/Kt.h>

int main(int argc, const char * argv[]) {
    @autoreleasepool {
        KtSpecificItem *item = [[KtSpecificItem alloc] initWithId:42 tag:@"test"];
        NSString *desc = [item describe];
        if (![desc isEqualToString:@"Item #42"]) {
            NSLog(@"Expected 'Item #42', got '%@'", desc);
            return 1;
        }

        printf("OK\n");
    }
    return 0;
}
