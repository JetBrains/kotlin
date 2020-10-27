import kotlinx.cinterop.*
import kotlin.test.*
import objcTests.*

@Test fun testAllocNoRetain() {
    // Ensure that calling Kotlin constructor generated for Objective-C initializer doesn't result in
    // redundant retain-release sequence for `alloc` result, since it may provoke specific bugs to reproduce, e.g.
    // the one found in [[NSOutputStream alloc] initToMemory] sequence where initToMemory deallocates its receiver
    // forcibly when replacing it with other object: (to be compiled with ARC enabled)
    /*
    #import <Foundation/Foundation.h>

    void* mem;
    NSOutputStream* allocated = nil;

    int main() {
        allocated = [NSOutputStream alloc];
        NSOutputStream* initialized = [allocated initToMemory];
        mem = calloc(1, 0x10); // To corrupt the 'allocated' object header.
        allocated = nil; // Crashes here in objc_release.

        return 0;
    }
     */

    assertTrue(TestAllocNoRetain().ok)
}