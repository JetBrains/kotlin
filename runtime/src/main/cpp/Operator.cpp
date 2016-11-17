#include <math.h>

#include "Natives.h"


extern "C" {

//--- Char --------------------------------------------------------------------//

KInt    Kotlin_Char_compareTo_Char   (KChar a, KChar   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KChar   Kotlin_Char_plus_Char        (KChar a, KInt    b) { return a + b; }
KChar   Kotlin_Char_minus_Char       (KChar a, KChar   b) { return a - b; }
KChar   Kotlin_Char_minus_Char       (KChar a, KInt    b) { return a - b; }
KChar   Kotlin_Char_inc              (KChar a           ) { return a + 1; }
KChar   Kotlin_Char_dec              (KChar a           ) { return a - 1; }

KByte   Kotlin_Char_toByte           (KChar a           ) { return a; }
KChar   Kotlin_Char_toChar           (KChar a           ) { return a; }
KShort  Kotlin_Char_toShort          (KChar a           ) { return a; }
KInt    Kotlin_Char_toInt            (KChar a           ) { return a; }
KLong   Kotlin_Char_toLong           (KChar a           ) { return a; }
KFloat  Kotlin_Char_toFloat          (KChar a           ) { return a; }
KDouble Kotlin_Char_toDouble         (KChar a           ) { return a; }

//--- Byte --------------------------------------------------------------------//

KInt    Kotlin_Byte_compareTo_Byte   (KByte a, KByte   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Byte_compareTo_Short  (KByte a, KShort  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Byte_compareTo_Int    (KByte a, KInt    b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Byte_compareTo_Long   (KByte a, KLong   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Byte_compareTo_Float  (KByte a, KFloat  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Byte_compareTo_Double (KByte a, KDouble b) { if (a == b) return 0; return (a < b) ? -1 : 1; }

KInt    Kotlin_Byte_plus_Byte        (KByte a, KByte   b) { return a + b; }
KInt    Kotlin_Byte_plus_Short       (KByte a, KShort  b) { return a + b; }
KInt    Kotlin_Byte_plus_Int         (KByte a, KInt    b) { return a + b; }
KLong   Kotlin_Byte_plus_Long        (KByte a, KLong   b) { return a + b; }
KFloat  Kotlin_Byte_plus_Float       (KByte a, KFloat  b) { return a + b; }
KDouble Kotlin_Byte_plus_Double      (KByte a, KDouble b) { return a + b; }

KInt    Kotlin_Byte_minus_Byte       (KByte a, KByte   b) { return a - b; }
KInt    Kotlin_Byte_minus_Short      (KByte a, KShort  b) { return a - b; }
KInt    Kotlin_Byte_minus_Int        (KByte a, KInt    b) { return a - b; }
KLong   Kotlin_Byte_minus_Long       (KByte a, KLong   b) { return a - b; }
KFloat  Kotlin_Byte_minus_Float      (KByte a, KFloat  b) { return a - b; }
KDouble Kotlin_Byte_minus_Double     (KByte a, KDouble b) { return a - b; }

KInt    Kotlin_Byte_div_Byte         (KByte a, KByte   b) { return a / b; }
KInt    Kotlin_Byte_div_Short        (KByte a, KShort  b) { return a / b; }
KInt    Kotlin_Byte_div_Int          (KByte a, KInt    b) { return a / b; }
KLong   Kotlin_Byte_div_Long         (KByte a, KLong   b) { return a / b; }
KFloat  Kotlin_Byte_div_Float        (KByte a, KFloat  b) { return a / b; }
KDouble Kotlin_Byte_div_Double       (KByte a, KDouble b) { return a / b; }

KInt    Kotlin_Byte_mod_Byte         (KByte a, KByte   b) { return a % b; }
KInt    Kotlin_Byte_mod_Short        (KByte a, KShort  b) { return a % b; }
KInt    Kotlin_Byte_mod_Int          (KByte a, KInt    b) { return a % b; }
KLong   Kotlin_Byte_mod_Long         (KByte a, KLong   b) { return a % b; }
KFloat  Kotlin_Byte_mod_Float        (KByte a, KFloat  b) { return fmodf(a, b); }
KDouble Kotlin_Byte_mod_Double       (KByte a, KDouble b) { return fmod (a, b); }

KInt    Kotlin_Byte_times_Byte       (KByte a, KByte   b) { return a * b; }
KInt    Kotlin_Byte_times_Short      (KByte a, KShort  b) { return a * b; }
KInt    Kotlin_Byte_times_Int        (KByte a, KInt    b) { return a * b; }
KLong   Kotlin_Byte_times_Long       (KByte a, KLong   b) { return a * b; }
KFloat  Kotlin_Byte_times_Float      (KByte a, KFloat  b) { return a * b; }
KDouble Kotlin_Byte_times_Double     (KByte a, KDouble b) { return a * b; }

KByte   Kotlin_Byte_inc              (KByte a           ) { return ++a; }
KByte   Kotlin_Byte_dec              (KByte a           ) { return --a; }
KInt    Kotlin_Byte_unaryPlus        (KByte a           ) { return  +a; }
KInt    Kotlin_Byte_unaryMinus       (KByte a           ) { return  -a; }

KByte   Kotlin_Byte_toByte           (KByte a           ) { return a; }
KShort  Kotlin_Byte_toShort          (KByte a           ) { return a; }
KInt    Kotlin_Byte_toInt            (KByte a           ) { return a; }
KLong   Kotlin_Byte_toLong           (KByte a           ) { return a; }
KFloat  Kotlin_Byte_toFloat          (KByte a           ) { return a; }
KDouble Kotlin_Byte_toDouble         (KByte a           ) { return a; }

//--- Short -------------------------------------------------------------------//

KInt    Kotlin_Short_compareTo_Byte   (KShort a, KByte   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Short_compareTo_Short  (KShort a, KShort  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Short_compareTo_Int    (KShort a, KInt    b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Short_compareTo_Long   (KShort a, KLong   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Short_compareTo_Float  (KShort a, KFloat  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Short_compareTo_Double (KShort a, KDouble b) { if (a == b) return 0; return (a < b) ? -1 : 1; }

KInt    Kotlin_Short_plus_Byte        (KShort a, KByte   b) { return a + b; }
KInt    Kotlin_Short_plus_Short       (KShort a, KShort  b) { return a + b; }
KInt    Kotlin_Short_plus_Int         (KShort a, KInt    b) { return a + b; }
KLong   Kotlin_Short_plus_Long        (KShort a, KLong   b) { return a + b; }
KFloat  Kotlin_Short_plus_Float       (KShort a, KFloat  b) { return a + b; }
KDouble Kotlin_Short_plus_Double      (KShort a, KDouble b) { return a + b; }

KInt    Kotlin_Short_minus_Byte       (KShort a, KByte   b) { return a - b; }
KInt    Kotlin_Short_minus_Short      (KShort a, KShort  b) { return a - b; }
KInt    Kotlin_Short_minus_Int        (KShort a, KInt    b) { return a - b; }
KLong   Kotlin_Short_minus_Long       (KShort a, KLong   b) { return a - b; }
KFloat  Kotlin_Short_minus_Float      (KShort a, KFloat  b) { return a - b; }
KDouble Kotlin_Short_minus_Double     (KShort a, KDouble b) { return a - b; }

KInt    Kotlin_Short_div_Byte         (KShort a, KByte   b) { return a / b; }
KInt    Kotlin_Short_div_Short        (KShort a, KShort  b) { return a / b; }
KInt    Kotlin_Short_div_Int          (KShort a, KInt    b) { return a / b; }
KLong   Kotlin_Short_div_Long         (KShort a, KLong   b) { return a / b; }
KFloat  Kotlin_Short_div_Float        (KShort a, KFloat  b) { return a / b; }
KDouble Kotlin_Short_div_Double       (KShort a, KDouble b) { return a / b; }

KInt    Kotlin_Short_mod_Byte         (KShort a, KByte   b) { return a % b; }
KInt    Kotlin_Short_mod_Short        (KShort a, KShort  b) { return a % b; }
KInt    Kotlin_Short_mod_Int          (KShort a, KInt    b) { return a % b; }
KLong   Kotlin_Short_mod_Long         (KShort a, KLong   b) { return a % b; }
KFloat  Kotlin_Short_mod_Float        (KShort a, KFloat  b) { return fmodf(a, b); }
KDouble Kotlin_Short_mod_Double       (KShort a, KDouble b) { return fmod (a, b); }

KInt    Kotlin_Short_times_Byte       (KShort a, KByte   b) { return a * b; }
KInt    Kotlin_Short_times_Short      (KShort a, KShort  b) { return a * b; }
KInt    Kotlin_Short_times_Int        (KShort a, KInt    b) { return a * b; }
KLong   Kotlin_Short_times_Long       (KShort a, KLong   b) { return a * b; }
KFloat  Kotlin_Short_times_Float      (KShort a, KFloat  b) { return a * b; }
KDouble Kotlin_Short_times_Double     (KShort a, KDouble b) { return a * b; }

KShort  Kotlin_Short_inc              (KShort a           ) { return ++a; }
KShort  Kotlin_Short_dec              (KShort a           ) { return --a; }
KInt    Kotlin_Short_unaryPlus        (KShort a           ) { return  +a; }
KInt    Kotlin_Short_unaryMinus       (KShort a           ) { return  -a; }

KByte   Kotlin_Short_toByte           (KShort a           ) { return a; }
KShort  Kotlin_Short_toShort          (KShort a           ) { return a; }
KInt    Kotlin_Short_toInt            (KShort a           ) { return a; }
KLong   Kotlin_Short_toLong           (KShort a           ) { return a; }
KFloat  Kotlin_Short_toFloat          (KShort a           ) { return a; }
KDouble Kotlin_Short_toDouble         (KShort a           ) { return a; }

//--- Int ---------------------------------------------------------------------//

KInt    Kotlin_Int_compareTo_Byte   (KInt a, KByte   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Int_compareTo_Short  (KInt a, KShort  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Int_compareTo_Int    (KInt a, KInt    b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Int_compareTo_Long   (KInt a, KLong   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Int_compareTo_Float  (KInt a, KFloat  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Int_compareTo_Double (KInt a, KDouble b) { if (a == b) return 0; return (a < b) ? -1 : 1; }

KInt    Kotlin_Int_plus_Byte        (KInt a, KByte   b) { return a + b; }
KInt    Kotlin_Int_plus_Short       (KInt a, KShort  b) { return a + b; }
KInt    Kotlin_Int_plus_Int         (KInt a, KInt    b) { return a + b; }
KLong   Kotlin_Int_plus_Long        (KInt a, KLong   b) { return a + b; }
KFloat  Kotlin_Int_plus_Float       (KInt a, KFloat  b) { return a + b; }
KDouble Kotlin_Int_plus_Double      (KInt a, KDouble b) { return a + b; }

KInt    Kotlin_Int_minus_Byte       (KInt a, KByte   b) { return a - b; }
KInt    Kotlin_Int_minus_Short      (KInt a, KShort  b) { return a - b; }
KInt    Kotlin_Int_minus_Int        (KInt a, KInt    b) { return a - b; }
KLong   Kotlin_Int_minus_Long       (KInt a, KLong   b) { return a - b; }
KFloat  Kotlin_Int_minus_Float      (KInt a, KFloat  b) { return a - b; }
KDouble Kotlin_Int_minus_Double     (KInt a, KDouble b) { return a - b; }

KInt    Kotlin_Int_div_Byte         (KInt a, KByte   b) { return a / b; }
KInt    Kotlin_Int_div_Short        (KInt a, KShort  b) { return a / b; }
KInt    Kotlin_Int_div_Int          (KInt a, KInt    b) { return a / b; }
KLong   Kotlin_Int_div_Long         (KInt a, KLong   b) { return a / b; }
KFloat  Kotlin_Int_div_Float        (KInt a, KFloat  b) { return a / b; }
KDouble Kotlin_Int_div_Double       (KInt a, KDouble b) { return a / b; }

KInt    Kotlin_Int_mod_Byte         (KInt a, KByte   b) { return a % b; }
KInt    Kotlin_Int_mod_Short        (KInt a, KShort  b) { return a % b; }
KInt    Kotlin_Int_mod_Int          (KInt a, KInt    b) { return a % b; }
KLong   Kotlin_Int_mod_Long         (KInt a, KLong   b) { return a % b; }
KFloat  Kotlin_Int_mod_Float        (KInt a, KFloat  b) { return fmodf(a, b); }
KDouble Kotlin_Int_mod_Double       (KInt a, KDouble b) { return fmod (a, b); }

KInt    Kotlin_Int_times_Byte       (KInt a, KByte   b) { return a * b; }
KInt    Kotlin_Int_times_Short      (KInt a, KShort  b) { return a * b; }
KInt    Kotlin_Int_times_Int        (KInt a, KInt    b) { return a * b; }
KLong   Kotlin_Int_times_Long       (KInt a, KLong   b) { return a * b; }
KFloat  Kotlin_Int_times_Float      (KInt a, KFloat  b) { return a * b; }
KDouble Kotlin_Int_times_Double     (KInt a, KDouble b) { return a * b; }

KInt    Kotlin_Int_inc              (KInt a           ) { return ++a; }
KInt    Kotlin_Int_dec              (KInt a           ) { return --a; }
KInt    Kotlin_Int_unaryPlus        (KInt a           ) { return  +a; }
KInt    Kotlin_Int_unaryMinus       (KInt a           ) { return  -a; }

KByte   Kotlin_Int_toByte           (KInt a           ) { return a; }
KShort  Kotlin_Int_toShort          (KInt a           ) { return a; }
KInt    Kotlin_Int_toInt            (KInt a           ) { return a; }
KLong   Kotlin_Int_toLong           (KInt a           ) { return a; }
KFloat  Kotlin_Int_toFloat          (KInt a           ) { return a; }
KDouble Kotlin_Int_toDouble         (KInt a           ) { return a; }

//--- Long --------------------------------------------------------------------//

KInt    Kotlin_Long_compareTo_Byte   (KLong a, KByte   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Long_compareTo_Short  (KLong a, KShort  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Long_compareTo_Int    (KLong a, KInt    b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Long_compareTo_Long   (KLong a, KLong   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Long_compareTo_Float  (KLong a, KFloat  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Long_compareTo_Double (KLong a, KDouble b) { if (a == b) return 0; return (a < b) ? -1 : 1; }

KLong   Kotlin_Long_plus_Byte        (KLong a, KByte   b) { return a + b; }
KLong   Kotlin_Long_plus_Short       (KLong a, KShort  b) { return a + b; }
KLong   Kotlin_Long_plus_Int         (KLong a, KInt    b) { return a + b; }
KLong   Kotlin_Long_plus_Long        (KLong a, KLong   b) { return a + b; }
KFloat  Kotlin_Long_plus_Float       (KLong a, KFloat  b) { return a + b; }
KDouble Kotlin_Long_plus_Double      (KLong a, KDouble b) { return a + b; }

KLong   Kotlin_Long_minus_Byte       (KLong a, KByte   b) { return a - b; }
KLong   Kotlin_Long_minus_Short      (KLong a, KShort  b) { return a - b; }
KLong   Kotlin_Long_minus_Int        (KLong a, KInt    b) { return a - b; }
KLong   Kotlin_Long_minus_Long       (KLong a, KLong   b) { return a - b; }
KFloat  Kotlin_Long_minus_Float      (KLong a, KFloat  b) { return a - b; }
KDouble Kotlin_Long_minus_Double     (KLong a, KDouble b) { return a - b; }

KLong   Kotlin_Long_div_Byte         (KLong a, KByte   b) { return a / b; }
KLong   Kotlin_Long_div_Short        (KLong a, KShort  b) { return a / b; }
KLong   Kotlin_Long_div_Int          (KLong a, KInt    b) { return a / b; }
KLong   Kotlin_Long_div_Long         (KLong a, KLong   b) { return a / b; }
KFloat  Kotlin_Long_div_Float        (KLong a, KFloat  b) { return a / b; }
KDouble Kotlin_Long_div_Double       (KLong a, KDouble b) { return a / b; }

KLong   Kotlin_Long_mod_Byte         (KLong a, KByte   b) { return a % b; }
KLong   Kotlin_Long_mod_Short        (KLong a, KShort  b) { return a % b; }
KLong   Kotlin_Long_mod_Int          (KLong a, KInt    b) { return a % b; }
KLong   Kotlin_Long_mod_Long         (KLong a, KLong   b) { return a % b; }
KFloat  Kotlin_Long_mod_Float        (KLong a, KFloat  b) { return fmodf(a, b); }
KDouble Kotlin_Long_mod_Double       (KLong a, KDouble b) { return fmod (a, b); }

KLong   Kotlin_Long_times_Byte       (KLong a, KByte   b) { return a * b; }
KLong   Kotlin_Long_times_Short      (KLong a, KShort  b) { return a * b; }
KLong   Kotlin_Long_times_Int        (KLong a, KInt    b) { return a * b; }
KLong   Kotlin_Long_times_Long       (KLong a, KLong   b) { return a * b; }
KFloat  Kotlin_Long_times_Float      (KLong a, KFloat  b) { return a * b; }
KDouble Kotlin_Long_times_Double     (KLong a, KDouble b) { return a * b; }

KLong   Kotlin_Long_inc              (KLong a           ) { return ++a; }
KLong   Kotlin_Long_dec              (KLong a           ) { return --a; }
KLong   Kotlin_Long_unaryPlus        (KLong a           ) { return  +a; }
KLong   Kotlin_Long_unaryMinus       (KLong a           ) { return  -a; }

KByte   Kotlin_Long_toByte           (KLong a           ) { return a; }
KShort  Kotlin_Long_toShort          (KLong a           ) { return a; }
KInt    Kotlin_Long_toInt            (KLong a           ) { return a; }
KLong   Kotlin_Long_toLong           (KLong a           ) { return a; }
KFloat  Kotlin_Long_toFloat          (KLong a           ) { return a; }
KDouble Kotlin_Long_toDouble         (KLong a           ) { return a; }

//--- Float -------------------------------------------------------------------//

KInt    Kotlin_Float_compareTo_Byte   (KFloat a, KByte   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Float_compareTo_Short  (KFloat a, KShort  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Float_compareTo_Int    (KFloat a, KInt    b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Float_compareTo_Long   (KFloat a, KLong   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Float_compareTo_Float  (KFloat a, KFloat  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Float_compareTo_Double (KFloat a, KDouble b) { if (a == b) return 0; return (a < b) ? -1 : 1; }

KFloat  Kotlin_Float_plus_Byte        (KFloat a, KByte   b) { return a + b; }
KFloat  Kotlin_Float_plus_Short       (KFloat a, KShort  b) { return a + b; }
KFloat  Kotlin_Float_plus_Int         (KFloat a, KInt    b) { return a + b; }
KFloat  Kotlin_Float_plus_Long        (KFloat a, KLong   b) { return a + b; }
KFloat  Kotlin_Float_plus_Float       (KFloat a, KFloat  b) { return a + b; }
KDouble Kotlin_Float_plus_Double      (KFloat a, KDouble b) { return a + b; }

KFloat  Kotlin_Float_minus_Byte       (KFloat a, KByte   b) { return a - b; }
KFloat  Kotlin_Float_minus_Short      (KFloat a, KShort  b) { return a - b; }
KFloat  Kotlin_Float_minus_Int        (KFloat a, KInt    b) { return a - b; }
KFloat  Kotlin_Float_minus_Long       (KFloat a, KLong   b) { return a - b; }
KFloat  Kotlin_Float_minus_Float      (KFloat a, KFloat  b) { return a - b; }
KDouble Kotlin_Float_minus_Double     (KFloat a, KDouble b) { return a - b; }

KFloat  Kotlin_Float_div_Byte         (KFloat a, KByte   b) { return a / b; }
KFloat  Kotlin_Float_div_Short        (KFloat a, KShort  b) { return a / b; }
KFloat  Kotlin_Float_div_Int          (KFloat a, KInt    b) { return a / b; }
KFloat  Kotlin_Float_div_Long         (KFloat a, KLong   b) { return a / b; }
KFloat  Kotlin_Float_div_Float        (KFloat a, KFloat  b) { return a / b; }
KDouble Kotlin_Float_div_Double       (KFloat a, KDouble b) { return a / b; }

KFloat  Kotlin_Float_mod_Byte         (KFloat a, KByte   b) { return fmodf(a, b); }
KFloat  Kotlin_Float_mod_Short        (KFloat a, KShort  b) { return fmodf(a, b); }
KFloat  Kotlin_Float_mod_Int          (KFloat a, KInt    b) { return fmodf(a, b); }
KFloat  Kotlin_Float_mod_Long         (KFloat a, KLong   b) { return fmodf(a, b); }
KFloat  Kotlin_Float_mod_Float        (KFloat a, KFloat  b) { return fmodf(a, b); }
KDouble Kotlin_Float_mod_Double       (KFloat a, KDouble b) { return fmod (a, b); }

KFloat  Kotlin_Float_times_Byte       (KFloat a, KByte   b) { return a * b; }
KFloat  Kotlin_Float_times_Short      (KFloat a, KShort  b) { return a * b; }
KFloat  Kotlin_Float_times_Int        (KFloat a, KInt    b) { return a * b; }
KFloat  Kotlin_Float_times_Long       (KFloat a, KLong   b) { return a * b; }
KFloat  Kotlin_Float_times_Float      (KFloat a, KFloat  b) { return a * b; }
KDouble Kotlin_Float_times_Double     (KFloat a, KDouble b) { return a * b; }

KFloat  Kotlin_Float_inc              (KFloat a           ) { return ++a; }
KFloat  Kotlin_Float_dec              (KFloat a           ) { return --a; }
KFloat  Kotlin_Float_unaryPlus        (KFloat a           ) { return  +a; }
KFloat  Kotlin_Float_unaryMinus       (KFloat a           ) { return  -a; }

KByte   Kotlin_Float_toByte           (KFloat a           ) { return a; }
KShort  Kotlin_Float_toShort          (KFloat a           ) { return a; }
KInt    Kotlin_Float_toInt            (KFloat a           ) { return a; }
KLong   Kotlin_Float_toLong           (KFloat a           ) { return a; }
KFloat  Kotlin_Float_toFloat          (KFloat a           ) { return a; }
KDouble Kotlin_Float_toDouble         (KFloat a           ) { return a; }

//--- Double ------------------------------------------------------------------//

KInt    Kotlin_Double_compareTo_Byte   (KDouble a, KByte   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Double_compareTo_Short  (KDouble a, KShort  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Double_compareTo_Int    (KDouble a, KInt    b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Double_compareTo_Long   (KDouble a, KLong   b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Double_compareTo_Float  (KDouble a, KFloat  b) { if (a == b) return 0; return (a < b) ? -1 : 1; }
KInt    Kotlin_Double_compareTo_Double (KDouble a, KDouble b) { if (a == b) return 0; return (a < b) ? -1 : 1; }

KDouble Kotlin_Double_plus_Byte        (KDouble a, KByte   b) { return a + b; }
KDouble Kotlin_Double_plus_Short       (KDouble a, KShort  b) { return a + b; }
KDouble Kotlin_Double_plus_Int         (KDouble a, KInt    b) { return a + b; }
KDouble Kotlin_Double_plus_Long        (KDouble a, KLong   b) { return a + b; }
KDouble Kotlin_Double_plus_Float       (KDouble a, KFloat  b) { return a + b; }
KDouble Kotlin_Double_plus_Double      (KDouble a, KDouble b) { return a + b; }

KDouble Kotlin_Double_minus_Byte       (KDouble a, KByte   b) { return a - b; }
KDouble Kotlin_Double_minus_Short      (KDouble a, KShort  b) { return a - b; }
KDouble Kotlin_Double_minus_Int        (KDouble a, KInt    b) { return a - b; }
KDouble Kotlin_Double_minus_Long       (KDouble a, KLong   b) { return a - b; }
KDouble Kotlin_Double_minus_Float      (KDouble a, KFloat  b) { return a - b; }
KDouble Kotlin_Double_minus_Double     (KDouble a, KDouble b) { return a - b; }

KDouble Kotlin_Double_div_Byte         (KDouble a, KByte   b) { return a / b; }
KDouble Kotlin_Double_div_Short        (KDouble a, KShort  b) { return a / b; }
KDouble Kotlin_Double_div_Int          (KDouble a, KInt    b) { return a / b; }
KDouble Kotlin_Double_div_Long         (KDouble a, KLong   b) { return a / b; }
KDouble Kotlin_Double_div_Float        (KDouble a, KFloat  b) { return a / b; }
KDouble Kotlin_Double_div_Double       (KDouble a, KDouble b) { return a / b; }

KDouble Kotlin_Double_mod_Byte         (KDouble a, KByte   b) { return fmod(a, b); }
KDouble Kotlin_Double_mod_Short        (KDouble a, KShort  b) { return fmod(a, b); }
KDouble Kotlin_Double_mod_Int          (KDouble a, KInt    b) { return fmod(a, b); }
KDouble Kotlin_Double_mod_Long         (KDouble a, KLong   b) { return fmod(a, b); }
KDouble Kotlin_Double_mod_Float        (KDouble a, KFloat  b) { return fmod(a, b); }
KDouble Kotlin_Double_mod_Double       (KDouble a, KDouble b) { return fmod(a, b); }

KDouble Kotlin_Double_times_Byte       (KDouble a, KByte   b) { return a * b; }
KDouble Kotlin_Double_times_Short      (KDouble a, KShort  b) { return a * b; }
KDouble Kotlin_Double_times_Int        (KDouble a, KInt    b) { return a * b; }
KDouble Kotlin_Double_times_Long       (KDouble a, KLong   b) { return a * b; }
KDouble Kotlin_Double_times_Float      (KDouble a, KFloat  b) { return a * b; }
KDouble Kotlin_Double_times_Double     (KDouble a, KDouble b) { return a * b; }

KDouble Kotlin_Double_inc              (KDouble a           ) { return ++a; }
KDouble Kotlin_Double_dec              (KDouble a           ) { return --a; }
KDouble Kotlin_Double_unaryPlus        (KDouble a           ) { return  +a; }
KDouble Kotlin_Double_unaryMinus       (KDouble a           ) { return  -a; }

KByte   Kotlin_Double_toByte           (KDouble a           ) { return a; }
KShort  Kotlin_Double_toShort          (KDouble a           ) { return a; }
KInt    Kotlin_Double_toInt            (KDouble a           ) { return a; }
KLong   Kotlin_Double_toLong           (KDouble a           ) { return a; }
KFloat  Kotlin_Double_toFloat          (KDouble a           ) { return a; }
KDouble Kotlin_Double_toDouble         (KDouble a           ) { return a; }
}  // extern "C"