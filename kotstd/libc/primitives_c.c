#define MAKE_CONVERT(from, from_type, to, to_type) to_type kotlinclib_ ## from ## To ## to  ( from_type value )  __attribute__(( weak)) { return (to_type) value;}


MAKE_CONVERT(int, int, Byte, char)
MAKE_CONVERT(int, int, Char, char)
MAKE_CONVERT(int, int, Short, short)
MAKE_CONVERT(int, int, Long, long)
MAKE_CONVERT(int, int, Float, float)
MAKE_CONVERT(int, int, Double, double)


MAKE_CONVERT(byte, char, Char, char)
MAKE_CONVERT(byte, char, Short, short)
MAKE_CONVERT(byte, char, Int, int)
MAKE_CONVERT(byte, char, Long, long)
MAKE_CONVERT(byte, char, Float, float)
MAKE_CONVERT(byte, char, Double, double)


MAKE_CONVERT(char, char, Byte, char)
MAKE_CONVERT(char, char, Short, short)
MAKE_CONVERT(char, char, Int, int)
MAKE_CONVERT(char, char, Long, long)
MAKE_CONVERT(char, char, Float, float)
MAKE_CONVERT(char, char, Double, double)


MAKE_CONVERT(short, short, Byte, char)
MAKE_CONVERT(short, short, Char, char)
MAKE_CONVERT(short, short, Int, int)
MAKE_CONVERT(short, short, Long, long)
MAKE_CONVERT(short, short, Float, float)
MAKE_CONVERT(short, short, Double, double)


MAKE_CONVERT(long, long, Byte, char)
MAKE_CONVERT(long, long, Char, char)
MAKE_CONVERT(long, long, Short, short)
MAKE_CONVERT(long, long, Int, int)
MAKE_CONVERT(long, long, Float, float)
MAKE_CONVERT(long, long, Double, double)


MAKE_CONVERT(float, float, Byte, char)
MAKE_CONVERT(float, float, Char, char)
MAKE_CONVERT(float, float, Short, short)
MAKE_CONVERT(float, float, Int, int)
MAKE_CONVERT(float, float, Long, long)
MAKE_CONVERT(float, float, Double, double)


MAKE_CONVERT(double, double, Byte, char)
MAKE_CONVERT(double, double, Char, char)
MAKE_CONVERT(double, double, Short, short)
MAKE_CONVERT(double, double, Int, int)
MAKE_CONVERT(double, double, Long, long)
MAKE_CONVERT(double, double, Float, float)


int kotlinclib_int_size() __attribute__((weak)){
  return sizeof (int);
}

int kotlinclib_long_size() __attribute__((weak)){
  return sizeof (long long);
}

int kotlinclib_boolean_size() __attribute__((weak)){
  return sizeof (char);
}

int kotlinclib_short_size() __attribute__((weak)){
  return sizeof (short);
}

int kotlinclib_double_size() __attribute__((weak)){
  return sizeof (double);
}

int kotlinclib_float_size() __attribute__((weak)){
  return sizeof (float);
}

int kotlinclib_char_size() __attribute__((weak)){
  return sizeof (char);
}

int kotlinclib_byte_size() __attribute__((weak)){
  return sizeof (char);
}