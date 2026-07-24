#ifndef __C_H__
#define __C_H__

int C;

// `C_getter` collides with the bridge stub name derived from the global `C`
// (whose getter accessor goes through `generateBridgeSymbol("knifunptr", "C_getter")`).
// The two calls exercise the per-key occurrence counter: one returns `..._C_getter_0`,
// the other `..._C_getter_1`.
int C_getter(void);

#endif
