#ifndef _IOKIT_IOHIDLIB_H
#define _IOKIT_IOHIDLIB_H

typedef struct IOGPoint {
    int      x;
} IOGPoint;

typedef enum {
    NX_OneButton,
} NXMouseButton;

extern int IOHIDPostEvent( IOGPoint            location );
extern int IOHIDGetButtonEventNum( NXMouseButton button );

#endif /* ! _IOKIT_IOHIDLIB_H */
