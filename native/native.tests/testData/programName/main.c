#include <errno.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char *argv[]) {
    printf("calling exec...\n");
    fflush(NULL);

    // Kotlin executable name is in argv[1]
    // Forward argv[2..n] to kotlin executable as arguments (the first one should be the programName according to posix)
    execv(argv[1], &(argv[2]));

    printf("exec failed with errno=%d\n", errno);
    return 1;
}