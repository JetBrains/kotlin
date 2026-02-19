mac:
	clang -dynamiclib -fPIC -o libs/mac/libmylib.dylib src/mylib.c

win:
	x86_64-w64-mingw32-gcc -shared -o libs/win/mylib.dll src/mylib.c -Wl,--out-implib,libs/win/libmylib.a

linux:
	gcc -shared -fPIC -o libs/linux/libmylib.so src/mylib.c
