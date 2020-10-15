/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <limits>
#include <string.h>

#include "KAssert.h"
#include "City.h"
#include "Exceptions.h"
#include "Memory.h"
#include "Natives.h"
#include "KString.h"
#include "Porting.h"
#include "Types.h"

#include "utf8.h"

#include "polyhash/PolyHash.h"

namespace {

typedef std::back_insert_iterator<KStdString> KStdStringInserter;
typedef KChar* utf8to16(const char*, const char*, KChar*);
typedef KStdStringInserter utf16to8(const KChar*,const KChar*, KStdStringInserter);

KStdStringInserter utf16toUtf8OrThrow(const KChar* start, const KChar* end, KStdStringInserter result) {
  TRY_CATCH(result = utf8::utf16to8(start, end, result),
            result = utf8::unchecked::utf16to8(start, end, result),
            ThrowCharacterCodingException());
  return result;
}

template<utf8to16 conversion>
OBJ_GETTER(utf8ToUtf16Impl, const char* rawString, const char* end, uint32_t charCount) {
  if (rawString == nullptr) RETURN_OBJ(nullptr);
  ArrayHeader* result = AllocArrayInstance(theStringTypeInfo, charCount, OBJ_RESULT)->array();
  KChar* rawResult = CharArrayAddressOfElementAt(result, 0);
  conversion(rawString, end, rawResult);
  RETURN_OBJ(result->obj());
}

template<utf16to8 conversion>
OBJ_GETTER(unsafeUtf16ToUtf8Impl, KString thiz, KInt start, KInt size) {
  RuntimeAssert(thiz->type_info() == theStringTypeInfo, "Must use String");
  const KChar* utf16 = CharArrayAddressOfElementAt(thiz, start);
  KStdString utf8;
  utf8.reserve(size);
  conversion(utf16, utf16 + size, back_inserter(utf8));
  ArrayHeader* result = AllocArrayInstance(theByteArrayTypeInfo, utf8.size(), OBJ_RESULT)->array();
  ::memcpy(ByteArrayAddressOfElementAt(result, 0), utf8.c_str(), utf8.size());
  RETURN_OBJ(result->obj());
}

OBJ_GETTER(utf8ToUtf16OrThrow, const char* rawString, size_t rawStringLength) {
  const char* end = rawString + rawStringLength;
  uint32_t charCount;
  TRY_CATCH(charCount = utf8::utf16_length(rawString, end),
            charCount = utf8::unchecked::utf16_length(rawString, end),
            ThrowCharacterCodingException());
  RETURN_RESULT_OF(utf8ToUtf16Impl<utf8::unchecked::utf8to16>, rawString, end, charCount);
}

OBJ_GETTER(utf8ToUtf16, const char* rawString, size_t rawStringLength) {
  const char* end = rawString + rawStringLength;
  uint32_t charCount = utf8::with_replacement::utf16_length(rawString, end);
  RETURN_RESULT_OF(utf8ToUtf16Impl<utf8::with_replacement::utf8to16>, rawString, end, charCount);
}

constexpr KShort uppercaseValuesCache[] = {
  924, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196,
  197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212,
  213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 192, 193, 194, 195, 196,
  197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212,
  213, 214, 247, 216, 217, 218, 219, 220, 221, 222, 376, 256, 256, 258, 258, 260,
  260, 262, 262, 264, 264, 266, 266, 268, 268, 270, 270, 272, 272, 274, 274, 276,
  276, 278, 278, 280, 280, 282, 282, 284, 284, 286, 286, 288, 288, 290, 290, 292,
  292, 294, 294, 296, 296, 298, 298, 300, 300, 302, 302, 304, 73, 306, 306, 308,
  308, 310, 310, 312, 313, 313, 315, 315, 317, 317, 319, 319, 321, 321, 323, 323,
  325, 325, 327, 327, 329, 330, 330, 332, 332, 334, 334, 336, 336, 338, 338, 340,
  340, 342, 342, 344, 344, 346, 346, 348, 348, 350, 350, 352, 352, 354, 354, 356,
  356, 358, 358, 360, 360, 362, 362, 364, 364, 366, 366, 368, 368, 370, 370, 372,
  372, 374, 374, 376, 377, 377, 379, 379, 381, 381, 83, 384, 385, 386, 386, 388,
  388, 390, 391, 391, 393, 394, 395, 395, 397, 398, 399, 400, 401, 401, 403, 404,
  502, 406, 407, 408, 408, 410, 411, 412, 413, 544, 415, 416, 416, 418, 418, 420,
  420, 422, 423, 423, 425, 426, 427, 428, 428, 430, 431, 431, 433, 434, 435, 435,
  437, 437, 439, 440, 440, 442, 443, 444, 444, 446, 503, 448, 449, 450, 451, 452,
  452, 452, 455, 455, 455, 458, 458, 458, 461, 461, 463, 463, 465, 465, 467, 467,
  469, 469, 471, 471, 473, 473, 475, 475, 398, 478, 478, 480, 480, 482, 482, 484,
  484, 486, 486, 488, 488, 490, 490, 492, 492, 494, 494, 496, 497, 497, 497, 500,
  500, 502, 503, 504, 504, 506, 506, 508, 508, 510, 510, 512, 512, 514, 514, 516,
  516, 518, 518, 520, 520, 522, 522, 524, 524, 526, 526, 528, 528, 530, 530, 532,
  532, 534, 534, 536, 536, 538, 538, 540, 540, 542, 542, 544, 545, 546, 546, 548,
  548, 550, 550, 552, 552, 554, 554, 556, 556, 558, 558, 560, 560, 562, 562, 564,
  565, 566, 567, 568, 569, 570, 571, 572, 573, 574, 575, 576, 577, 578, 579, 580,
  581, 582, 583, 584, 585, 586, 587, 588, 589, 590, 591, 592, 593, 594, 385, 390,
  597, 393, 394, 600, 399, 602, 400, 604, 605, 606, 607, 403, 609, 610, 404, 612,
  613, 614, 615, 407, 406, 618, 619, 620, 621, 622, 412, 624, 625, 413, 627, 628,
  415, 630, 631, 632, 633, 634, 635, 636, 637, 638, 639, 422, 641, 642, 425, 644,
  645, 646, 647, 430, 649, 433, 434, 652, 653, 654, 655, 656, 657, 439, 659, 660,
  661, 662, 663, 664, 665, 666, 667, 668, 669, 670, 671, 672, 673, 674, 675, 676,
  677, 678, 679, 680, 681, 682, 683, 684, 685, 686, 687, 688, 689, 690, 691, 692,
  693, 694, 695, 696, 697, 698, 699, 700, 701, 702, 703, 704, 705, 706, 707, 708,
  709, 710, 711, 712, 713, 714, 715, 716, 717, 718, 719, 720, 721, 722, 723, 724,
  725, 726, 727, 728, 729, 730, 731, 732, 733, 734, 735, 736, 737, 738, 739, 740,
  741, 742, 743, 744, 745, 746, 747, 748, 749, 750, 751, 752, 753, 754, 755, 756,
  757, 758, 759, 760, 761, 762, 763, 764, 765, 766, 767, 768, 769, 770, 771, 772,
  773, 774, 775, 776, 777, 778, 779, 780, 781, 782, 783, 784, 785, 786, 787, 788,
  789, 790, 791, 792, 793, 794, 795, 796, 797, 798, 799, 800, 801, 802, 803, 804,
  805, 806, 807, 808, 809, 810, 811, 812, 813, 814, 815, 816, 817, 818, 819, 820,
  821, 822, 823, 824, 825, 826, 827, 828, 829, 830, 831, 832, 833, 834, 835, 836,
  921, 838, 839, 840, 841, 842, 843, 844, 845, 846, 847, 848, 849, 850, 851, 852,
  853, 854, 855, 856, 857, 858, 859, 860, 861, 862, 863, 864, 865, 866, 867, 868,
  869, 870, 871, 872, 873, 874, 875, 876, 877, 878, 879, 880, 881, 882, 883, 884,
  885, 886, 887, 888, 889, 890, 891, 892, 893, 894, 895, 896, 897, 898, 899, 900,
  901, 902, 903, 904, 905, 906, 907, 908, 909, 910, 911, 912, 913, 914, 915, 916,
  917, 918, 919, 920, 921, 922, 923, 924, 925, 926, 927, 928, 929, 930, 931, 932,
  933, 934, 935, 936, 937, 938, 939, 902, 904, 905, 906, 944, 913, 914, 915, 916,
  917, 918, 919, 920, 921, 922, 923, 924, 925, 926, 927, 928, 929, 931, 931, 932,
  933, 934, 935, 936, 937, 938, 939, 908, 910, 911, 975, 914, 920, 978, 979, 980,
  934, 928, 983, 984, 984, 986, 986, 988, 988, 990, 990, 992, 992, 994, 994, 996,
  996, 998, 998
};

constexpr KShort lowercaseValuesCache[] = {
  224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239,
  240, 241, 242, 243, 244, 245, 246, 215, 248, 249, 250, 251, 252, 253, 254, 223,
  224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239,
  240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255,
  257, 257, 259, 259, 261, 261, 263, 263, 265, 265, 267, 267, 269, 269, 271, 271,
  273, 273, 275, 275, 277, 277, 279, 279, 281, 281, 283, 283, 285, 285, 287, 287,
  289, 289, 291, 291, 293, 293, 295, 295, 297, 297, 299, 299, 301, 301, 303, 303,
  105, 305, 307, 307, 309, 309, 311, 311, 312, 314, 314, 316, 316, 318, 318, 320,
  320, 322, 322, 324, 324, 326, 326, 328, 328, 329, 331, 331, 333, 333, 335, 335,
  337, 337, 339, 339, 341, 341, 343, 343, 345, 345, 347, 347, 349, 349, 351, 351,
  353, 353, 355, 355, 357, 357, 359, 359, 361, 361, 363, 363, 365, 365, 367, 367,
  369, 369, 371, 371, 373, 373, 375, 375, 255, 378, 378, 380, 380, 382, 382, 383,
  384, 595, 387, 387, 389, 389, 596, 392, 392, 598, 599, 396, 396, 397, 477, 601,
  603, 402, 402, 608, 611, 405, 617, 616, 409, 409, 410, 411, 623, 626, 414, 629,
  417, 417, 419, 419, 421, 421, 640, 424, 424, 643, 426, 427, 429, 429, 648, 432,
  432, 650, 651, 436, 436, 438, 438, 658, 441, 441, 442, 443, 445, 445, 446, 447,
  448, 449, 450, 451, 454, 454, 454, 457, 457, 457, 460, 460, 460, 462, 462, 464,
  464, 466, 466, 468, 468, 470, 470, 472, 472, 474, 474, 476, 476, 477, 479, 479,
  481, 481, 483, 483, 485, 485, 487, 487, 489, 489, 491, 491, 493, 493, 495, 495,
  496, 499, 499, 499, 501, 501, 405, 447, 505, 505, 507, 507, 509, 509, 511, 511,
  513, 513, 515, 515, 517, 517, 519, 519, 521, 521, 523, 523, 525, 525, 527, 527,
  529, 529, 531, 531, 533, 533, 535, 535, 537, 537, 539, 539, 541, 541, 543, 543,
  414, 545, 547, 547, 549, 549, 551, 551, 553, 553, 555, 555, 557, 557, 559, 559,
  561, 561, 563, 563, 564, 565, 566, 567, 568, 569, 570, 571, 572, 573, 574, 575,
  576, 577, 578, 579, 580, 581, 582, 583, 584, 585, 586, 587, 588, 589, 590, 591,
  592, 593, 594, 595, 596, 597, 598, 599, 600, 601, 602, 603, 604, 605, 606, 607,
  608, 609, 610, 611, 612, 613, 614, 615, 616, 617, 618, 619, 620, 621, 622, 623,
  624, 625, 626, 627, 628, 629, 630, 631, 632, 633, 634, 635, 636, 637, 638, 639,
  640, 641, 642, 643, 644, 645, 646, 647, 648, 649, 650, 651, 652, 653, 654, 655,
  656, 657, 658, 659, 660, 661, 662, 663, 664, 665, 666, 667, 668, 669, 670, 671,
  672, 673, 674, 675, 676, 677, 678, 679, 680, 681, 682, 683, 684, 685, 686, 687,
  688, 689, 690, 691, 692, 693, 694, 695, 696, 697, 698, 699, 700, 701, 702, 703,
  704, 705, 706, 707, 708, 709, 710, 711, 712, 713, 714, 715, 716, 717, 718, 719,
  720, 721, 722, 723, 724, 725, 726, 727, 728, 729, 730, 731, 732, 733, 734, 735,
  736, 737, 738, 739, 740, 741, 742, 743, 744, 745, 746, 747, 748, 749, 750, 751,
  752, 753, 754, 755, 756, 757, 758, 759, 760, 761, 762, 763, 764, 765, 766, 767,
  768, 769, 770, 771, 772, 773, 774, 775, 776, 777, 778, 779, 780, 781, 782, 783,
  784, 785, 786, 787, 788, 789, 790, 791, 792, 793, 794, 795, 796, 797, 798, 799,
  800, 801, 802, 803, 804, 805, 806, 807, 808, 809, 810, 811, 812, 813, 814, 815,
  816, 817, 818, 819, 820, 821, 822, 823, 824, 825, 826, 827, 828, 829, 830, 831,
  832, 833, 834, 835, 836, 837, 838, 839, 840, 841, 842, 843, 844, 845, 846, 847,
  848, 849, 850, 851, 852, 853, 854, 855, 856, 857, 858, 859, 860, 861, 862, 863,
  864, 865, 866, 867, 868, 869, 870, 871, 872, 873, 874, 875, 876, 877, 878, 879,
  880, 881, 882, 883, 884, 885, 886, 887, 888, 889, 890, 891, 892, 893, 894, 895,
  896, 897, 898, 899, 900, 901, 940, 903, 941, 942, 943, 907, 972, 909, 973, 974,
  912, 945, 946, 947, 948, 949, 950, 951, 952, 953, 954, 955, 956, 957, 958, 959,
  960, 961, 930, 963, 964, 965, 966, 967, 968, 969, 970, 971, 940, 941, 942, 943,
  944, 945, 946, 947, 948, 949, 950, 951, 952, 953, 954, 955, 956, 957, 958, 959,
  960, 961, 962, 963, 964, 965, 966, 967, 968, 969, 970, 971, 972, 973, 974, 975,
  976, 977, 978, 979, 980, 981, 982, 983, 985, 985, 987, 987, 989, 989, 991, 991,
  993, 993, 995, 995, 997, 997, 999, 999
};

constexpr KChar uppercaseKeys[] = {
  0x61, 0xb5, 0xe0, 0xf8, 0xff, 0x101, 0x131, 0x133, 0x13a, 0x14b, 0x17a, 0x17f, 0x183, 0x188, 0x18c, 0x192, 0x195, 0x199, 0x1a1, 0x1a8,
  0x1ad, 0x1b0, 0x1b4, 0x1b9, 0x1bd, 0x1bf, 0x1c5, 0x1c6, 0x1c8, 0x1c9, 0x1cb, 0x1cc, 0x1ce, 0x1dd, 0x1df, 0x1f2, 0x1f3, 0x1f5, 0x1f9, 0x223,
  0x253, 0x254, 0x256, 0x259, 0x25b, 0x260, 0x263, 0x268, 0x269, 0x26f, 0x272, 0x275, 0x280, 0x283, 0x288, 0x28a, 0x292, 0x345, 0x3ac, 0x3ad,
  0x3b1, 0x3c2, 0x3c3, 0x3cc, 0x3cd, 0x3d0, 0x3d1, 0x3d5, 0x3d6, 0x3db, 0x3f0, 0x3f1, 0x3f2, 0x430, 0x450, 0x461, 0x48d, 0x4c2, 0x4c8, 0x4cc,
  0x4d1, 0x4f9, 0x561, 0x1e01, 0x1e9b, 0x1ea1, 0x1f00, 0x1f10, 0x1f20, 0x1f30, 0x1f40, 0x1f51, 0x1f60, 0x1f70, 0x1f72, 0x1f76, 0x1f78, 0x1f7a, 0x1f7c, 0x1f80,
  0x1f90, 0x1fa0, 0x1fb0, 0x1fb3, 0x1fbe, 0x1fc3, 0x1fd0, 0x1fe0, 0x1fe5, 0x1ff3, 0x2170, 0x24d0, 0xff41
};

constexpr KChar uppercaseValues[] = {
  0x7a, 0xffe0, 0xb5, 0x2e7, 0xf6, 0xffe0, 0xfe, 0xffe0, 0xff, 0x79, 0x812f, 0xffff, 0x131, 0xff18, 0x8137, 0xffff, 0x8148, 0xffff, 0x8177, 0xffff,
  0x817e, 0xffff, 0x17f, 0xfed4, 0x8185, 0xffff, 0x188, 0xffff, 0x18c, 0xffff, 0x192, 0xffff, 0x195, 0x61, 0x199, 0xffff, 0x81a5, 0xffff, 0x1a8, 0xffff,
  0x1ad, 0xffff, 0x1b0, 0xffff, 0x81b6, 0xffff, 0x1b9, 0xffff, 0x1bd, 0xffff, 0x1bf, 0x38, 0x1c5, 0xffff, 0x1c6, 0xfffe, 0x1c8, 0xffff, 0x1c9, 0xfffe,
  0x1cb, 0xffff, 0x1cc, 0xfffe, 0x81dc, 0xffff, 0x1dd, 0xffb1, 0x81ef, 0xffff, 0x1f2, 0xffff, 0x1f3, 0xfffe, 0x1f5, 0xffff, 0x821f, 0xffff, 0x8233, 0xffff,
  0x253, 0xff2e, 0x254, 0xff32, 0x257, 0xff33, 0x259, 0xff36, 0x25b, 0xff35, 0x260, 0xff33, 0x263, 0xff31, 0x268, 0xff2f, 0x269, 0xff2d, 0x26f, 0xff2d,
  0x272, 0xff2b, 0x275, 0xff2a, 0x280, 0xff26, 0x283, 0xff26, 0x288, 0xff26, 0x28b, 0xff27, 0x292, 0xff25, 0x345, 0x54, 0x3ac, 0xffda, 0x3af, 0xffdb,
  0x3c1, 0xffe0, 0x3c2, 0xffe1, 0x3cb, 0xffe0, 0x3cc, 0xffc0, 0x3ce, 0xffc1, 0x3d0, 0xffc2, 0x3d1, 0xffc7, 0x3d5, 0xffd1, 0x3d6, 0xffca, 0x83ef, 0xffff,
  0x3f0, 0xffaa, 0x3f1, 0xffb0, 0x3f2, 0xffb1, 0x44f, 0xffe0, 0x45f, 0xffb0, 0x8481, 0xffff, 0x84bf, 0xffff, 0x84c4, 0xffff, 0x4c8, 0xffff, 0x4cc, 0xffff,
  0x84f5, 0xffff, 0x4f9, 0xffff, 0x586, 0xffd0, 0x9e95, 0xffff, 0x1e9b, 0xffc5, 0x9ef9, 0xffff, 0x1f07, 0x8, 0x1f15, 0x8, 0x1f27, 0x8, 0x1f37, 0x8,
  0x1f45, 0x8, 0x9f57, 0x8, 0x1f67, 0x8, 0x1f71, 0x4a, 0x1f75, 0x56, 0x1f77, 0x64, 0x1f79, 0x80, 0x1f7b, 0x70, 0x1f7d, 0x7e, 0x1f87, 0x8,
  0x1f97, 0x8, 0x1fa7, 0x8, 0x1fb1, 0x8, 0x1fb3, 0x9, 0x1fbe, 0xe3db, 0x1fc3, 0x9, 0x1fd1, 0x8, 0x1fe1, 0x8, 0x1fe5, 0x7, 0x1ff3, 0x9,
  0x217f, 0xfff0, 0x24e9, 0xffe6, 0xff5a, 0xffe0
};

constexpr KChar lowercaseKeys[] = {
  0x41, 0xc0, 0xd8, 0x100, 0x130, 0x132, 0x139, 0x14a, 0x178, 0x179, 0x181, 0x182, 0x186, 0x187, 0x189, 0x18b, 0x18e, 0x18f, 0x190, 0x191,
  0x193, 0x194, 0x196, 0x197, 0x198, 0x19c, 0x19d, 0x19f, 0x1a0, 0x1a6, 0x1a7, 0x1a9, 0x1ac, 0x1ae, 0x1af, 0x1b1, 0x1b3, 0x1b7, 0x1b8, 0x1bc,
  0x1c4, 0x1c5, 0x1c7, 0x1c8, 0x1ca, 0x1cb, 0x1de, 0x1f1, 0x1f2, 0x1f6, 0x1f7, 0x1f8, 0x222, 0x386, 0x388, 0x38c, 0x38e, 0x391, 0x3a3, 0x3da,
  0x400, 0x410, 0x460, 0x48c, 0x4c1, 0x4c7, 0x4cb, 0x4d0, 0x4f8, 0x531, 0x1e00, 0x1ea0, 0x1f08, 0x1f18, 0x1f28, 0x1f38, 0x1f48, 0x1f59, 0x1f68, 0x1f88,
  0x1f98, 0x1fa8, 0x1fb8, 0x1fba, 0x1fbc, 0x1fc8, 0x1fcc, 0x1fd8, 0x1fda, 0x1fe8, 0x1fea, 0x1fec, 0x1ff8, 0x1ffa, 0x1ffc, 0x2126, 0x212a, 0x212b, 0x2160, 0x24b6
};

constexpr KChar lowercaseValues[] = {
  0x5a, 0x20, 0xd6, 0x20, 0xde, 0x20, 0x812e, 0x1, 0x130, 0xff39, 0x8136, 0x1, 0x8147, 0x1, 0x8176, 0x1, 0x178, 0xff87, 0x817d, 0x1,
  0x181, 0xd2, 0x8184, 0x1, 0x186, 0xce, 0x187, 0x1, 0x18a, 0xcd, 0x18b, 0x1, 0x18e, 0x4f, 0x18f, 0xca, 0x190, 0xcb, 0x191, 0x1,
  0x193, 0xcd, 0x194, 0xcf, 0x196, 0xd3, 0x197, 0xd1, 0x198, 0x1, 0x19c, 0xd3, 0x19d, 0xd5, 0x19f, 0xd6, 0x81a4, 0x1, 0x1a6, 0xda,
  0x1a7, 0x1, 0x1a9, 0xda, 0x1ac, 0x1, 0x1ae, 0xda, 0x1af, 0x1, 0x1b2, 0xd9, 0x81b5, 0x1, 0x1b7, 0xdb, 0x1b8, 0x1, 0x1bc, 0x1,
  0x1c4, 0x2, 0x1c5, 0x1, 0x1c7, 0x2, 0x1c8, 0x1, 0x1ca, 0x2, 0x81db, 0x1, 0x81ee, 0x1, 0x1f1, 0x2, 0x81f4, 0x1, 0x1f6, 0xff9f,
  0x1f7, 0xffc8, 0x821e, 0x1, 0x8232, 0x1, 0x386, 0x26, 0x38a, 0x25, 0x38c, 0x40, 0x38f, 0x3f, 0x3a1, 0x20, 0x3ab, 0x20, 0x83ee, 0x1,
  0x40f, 0x50, 0x42f, 0x20, 0x8480, 0x1, 0x84be, 0x1, 0x84c3, 0x1, 0x4c7, 0x1, 0x4cb, 0x1, 0x84f4, 0x1, 0x4f8, 0x1, 0x556, 0x30,
  0x9e94, 0x1, 0x9ef8, 0x1, 0x1f0f, 0xfff8, 0x1f1d, 0xfff8, 0x1f2f, 0xfff8, 0x1f3f, 0xfff8, 0x1f4d, 0xfff8, 0x9f5f, 0xfff8, 0x1f6f, 0xfff8, 0x1f8f, 0xfff8,
  0x1f9f, 0xfff8, 0x1faf, 0xfff8, 0x1fb9, 0xfff8, 0x1fbb, 0xffb6, 0x1fbc, 0xfff7, 0x1fcb, 0xffaa, 0x1fcc, 0xfff7, 0x1fd9, 0xfff8, 0x1fdb, 0xff9c, 0x1fe9, 0xfff8,
  0x1feb, 0xff90, 0x1fec, 0xfff9, 0x1ff9, 0xff80, 0x1ffb, 0xff82, 0x1ffc, 0xfff7, 0x2126, 0xe2a3, 0x212a, 0xdf41, 0x212b, 0xdfba, 0x216f, 0x10, 0x24cf, 0x1a
};

constexpr KChar digitKeys[] = {
  0x30, 0x41, 0x61, 0x660, 0x6f0, 0x966, 0x9e6, 0xa66, 0xae6, 0xb66, 0xbe7, 0xc66, 0xce6, 0xd66, 0xe50, 0xed0, 0xf20, 0x1040, 0x1369, 0x17e0,
  0x1810, 0xff10, 0xff21, 0xff41
};

constexpr KChar digitValues[] = {
  0x39, 0x30, 0x5a, 0x37, 0x7a, 0x57, 0x669, 0x660, 0x6f9, 0x6f0, 0x96f, 0x966, 0x9ef, 0x9e6, 0xa6f, 0xa66, 0xaef, 0xae6, 0xb6f, 0xb66,
  0xbef, 0xbe6, 0xc6f, 0xc66, 0xcef, 0xce6, 0xd6f, 0xd66, 0xe59, 0xe50, 0xed9, 0xed0, 0xf29, 0xf20, 0x1049, 0x1040, 0x1371, 0x1368, 0x17e9, 0x17e0,
  0x1819, 0x1810, 0xff19, 0xff10, 0xff3a, 0xff17, 0xff5a, 0xff37
};

KChar towupper_Konan(KChar ch) {
  // Optimized case for ASCII.
  if ('a' <= ch && ch <= 'z') {
    return ch - ('a' - 'A');
  }
  if (ch < 181) {
    return ch;
  }
  if (ch < 1000) {
    return uppercaseValuesCache[ch - 181];
  }
  int result = binarySearchRange(uppercaseKeys, ARRAY_SIZE(uppercaseKeys), ch);
  if (result >= 0) {
    bool by2 = false;
    KChar start = uppercaseKeys[result];
    KChar end = uppercaseValues[result * 2];
    if ((start & 0x8000) != (end & 0x8000)) {
      end ^= 0x8000;
      by2 = true;
    }
    if (ch <= end) {
      if (by2 && (ch & 1) != (start & 1)) {
        return ch;
      }
      KChar mapping = uppercaseValues[result * 2 + 1];
      return ch + mapping;
    }
  }
  return ch;
}

KChar towlower_Konan(KChar ch) {
  // Optimized case for ASCII.
  if ('A' <= ch && ch <= 'Z') {
    return ch + ('a' - 'A');
  }
  if (ch < 192) {
    return ch;
  }
  if (ch < 1000) {
    return lowercaseValuesCache[ch - 192];
  }

  int result = binarySearchRange(lowercaseKeys, ARRAY_SIZE(lowercaseKeys), ch);
  if (result >= 0) {
    bool by2 = false;
    KChar start = lowercaseKeys[result];
    KChar end = lowercaseValues[result * 2];
    if ((start & 0x8000) != (end & 0x8000)) {
      end ^= 0x8000;
      by2 = true;
    }
    if (ch <= end) {
      if (by2 && (ch & 1) != (start & 1)) {
        return ch;
      }
      KChar mapping = lowercaseValues[result * 2 + 1];
      return ch + mapping;
    }
  }
  return ch;
}

} // namespace

extern "C" {

OBJ_GETTER(CreateStringFromCString, const char* cstring) {
  RETURN_RESULT_OF(utf8ToUtf16, cstring, cstring ? strlen(cstring) : 0);
}

OBJ_GETTER(CreateStringFromUtf8, const char* utf8, uint32_t lengthBytes) {
  RETURN_RESULT_OF(utf8ToUtf16, utf8, lengthBytes);
}

char* CreateCStringFromString(KConstRef kref) {
  if (kref == nullptr) return nullptr;
  KString kstring = kref->array();
  const KChar* utf16 = CharArrayAddressOfElementAt(kstring, 0);
  KStdString utf8;
  utf8.reserve(kstring->count_);
  utf8::unchecked::utf16to8(utf16, utf16 + kstring->count_, back_inserter(utf8));
  char* result = reinterpret_cast<char*>(konan::calloc(1, utf8.size() + 1));
  ::memcpy(result, utf8.c_str(), utf8.size());
  return result;
}

void DisposeCString(char* cstring) {
  if (cstring) konan::free(cstring);
}

// String.kt
OBJ_GETTER(Kotlin_String_replace, KString thiz, KChar oldChar, KChar newChar, KBoolean ignoreCase) {
  auto count = thiz->count_;
  ArrayHeader* result = AllocArrayInstance(theStringTypeInfo, count, OBJ_RESULT)->array();
  const KChar* thizRaw = CharArrayAddressOfElementAt(thiz, 0);
  KChar* resultRaw = CharArrayAddressOfElementAt(result, 0);
  if (ignoreCase) {
    KChar oldCharLower = towlower_Konan(oldChar);
    for (uint32_t index = 0; index < count; ++index) {
      KChar thizChar = *thizRaw++;
      *resultRaw++ = towlower_Konan(thizChar) == oldCharLower ? newChar : thizChar;
    }
  } else {
    for (uint32_t index = 0; index < count; ++index) {
      KChar thizChar = *thizRaw++;
      *resultRaw++ = thizChar == oldChar ? newChar : thizChar;
    }
  }
  RETURN_OBJ(result->obj());
}

OBJ_GETTER(Kotlin_String_plusImpl, KString thiz, KString other) {
  RuntimeAssert(thiz != nullptr, "this cannot be null");
  RuntimeAssert(other != nullptr, "other cannot be null");
  RuntimeAssert(thiz->type_info() == theStringTypeInfo, "Must be a string");
  RuntimeAssert(other->type_info() == theStringTypeInfo, "Must be a string");
  RuntimeAssert(thiz->count_ <= static_cast<uint32_t>(std::numeric_limits<int32_t>::max()), "this cannot be this large");
  RuntimeAssert(other->count_ <= static_cast<uint32_t>(std::numeric_limits<int32_t>::max()), "other cannot be this large");
  // Since thiz and other sizes are bounded by int32_t max value, their sum cannot exceed uint32_t max value - 1.
  uint32_t result_length = thiz->count_ + other->count_;
  if (result_length > static_cast<uint32_t>(std::numeric_limits<int32_t>::max())) {
    ThrowArrayIndexOutOfBoundsException();
  }
  ArrayHeader* result = AllocArrayInstance(theStringTypeInfo, result_length, OBJ_RESULT)->array();
  memcpy(
      CharArrayAddressOfElementAt(result, 0),
      CharArrayAddressOfElementAt(thiz, 0),
      thiz->count_ * sizeof(KChar));
  memcpy(
      CharArrayAddressOfElementAt(result, thiz->count_),
      CharArrayAddressOfElementAt(other, 0),
      other->count_ * sizeof(KChar));
  RETURN_OBJ(result->obj());
}

OBJ_GETTER(Kotlin_String_toUpperCase, KString thiz) {
  auto count = thiz->count_;
  ArrayHeader* result = AllocArrayInstance(theStringTypeInfo, count, OBJ_RESULT)->array();
  const KChar* thizRaw = CharArrayAddressOfElementAt(thiz, 0);
  KChar* resultRaw = CharArrayAddressOfElementAt(result, 0);
  for (uint32_t index = 0; index < count; ++index) {
    *resultRaw++ = towupper_Konan(*thizRaw++);
  }
  RETURN_OBJ(result->obj());
}

OBJ_GETTER(Kotlin_String_toLowerCase, KString thiz) {
  auto count = thiz->count_;
  ArrayHeader* result = AllocArrayInstance(theStringTypeInfo, count, OBJ_RESULT)->array();
  const KChar* thizRaw = CharArrayAddressOfElementAt(thiz, 0);
  KChar* resultRaw = CharArrayAddressOfElementAt(result, 0);
  for (uint32_t index = 0; index < count; ++index) {
    *resultRaw++ = towlower_Konan(*thizRaw++);
  }
  RETURN_OBJ(result->obj());
}

OBJ_GETTER(Kotlin_String_unsafeStringFromCharArray, KConstRef thiz, KInt start, KInt size) {
  const ArrayHeader* array = thiz->array();
  RuntimeAssert(array->type_info() == theCharArrayTypeInfo, "Must use a char array");

  if (size == 0) {
    RETURN_RESULT_OF0(TheEmptyString);
  }

  ArrayHeader* result = AllocArrayInstance(theStringTypeInfo, size, OBJ_RESULT)->array();
  memcpy(CharArrayAddressOfElementAt(result, 0),
         CharArrayAddressOfElementAt(array, start),
         size * sizeof(KChar));
  RETURN_OBJ(result->obj());
}

OBJ_GETTER(Kotlin_String_toCharArray, KString string, KInt start, KInt size) {
  ArrayHeader* result = AllocArrayInstance(theCharArrayTypeInfo, size, OBJ_RESULT)->array();
  memcpy(CharArrayAddressOfElementAt(result, 0),
         CharArrayAddressOfElementAt(string, start),
         size * sizeof(KChar));
  RETURN_OBJ(result->obj());
}

OBJ_GETTER(Kotlin_String_subSequence, KString thiz, KInt startIndex, KInt endIndex) {
  if (startIndex < 0 || static_cast<uint32_t>(endIndex) > thiz->count_ || startIndex > endIndex) {
    // TODO: is it correct exception?
    ThrowArrayIndexOutOfBoundsException();
  }
  if (startIndex == endIndex) {
    RETURN_RESULT_OF0(TheEmptyString);
  }
  KInt length = endIndex - startIndex;
  ArrayHeader* result = AllocArrayInstance(theStringTypeInfo, length, OBJ_RESULT)->array();
  memcpy(CharArrayAddressOfElementAt(result, 0),
         CharArrayAddressOfElementAt(thiz, startIndex),
         length * sizeof(KChar));
  RETURN_OBJ(result->obj());
}

KInt Kotlin_String_compareTo(KString thiz, KString other) {
  int result = memcmp(
    CharArrayAddressOfElementAt(thiz, 0),
    CharArrayAddressOfElementAt(other, 0),
    (thiz->count_ < other->count_ ? thiz->count_ : other->count_) * sizeof(KChar));
  if (result != 0) return result;
  int diff = thiz->count_ - other->count_;
  if (diff == 0) return 0;
  return diff < 0 ? -1 : 1;
}

KInt Kotlin_String_compareToIgnoreCase(KString thiz, KConstRef other) {
  RuntimeAssert(thiz->type_info() == theStringTypeInfo &&
                other->type_info() == theStringTypeInfo, "Must be strings");
  // Important, due to literal internalization.
  KString otherString = other->array();
  if (thiz == otherString) return 0;
  auto count = thiz->count_ < otherString->count_ ? thiz->count_ : otherString->count_;
  const KChar* thizRaw = CharArrayAddressOfElementAt(thiz, 0);
  const KChar* otherRaw = CharArrayAddressOfElementAt(otherString, 0);
  for (uint32_t index = 0; index < count; ++index) {
    int diff = towlower_Konan(*thizRaw++) - towlower_Konan(*otherRaw++);
    if (diff != 0)
      return diff < 0 ? -1 : 1;
  }
  if (otherString->count_ == thiz->count_)
    return 0;
  else if (otherString->count_ > thiz->count_)
    return -1;
  else
    return 1;
}


KChar Kotlin_String_get(KString thiz, KInt index) {
  // We couldn't have created a string bigger than max KInt value.
  // So if index is < 0, conversion to an unsigned value would make it bigger
  // than the array size.
  if (static_cast<uint32_t>(index) >= thiz->count_) {
    ThrowArrayIndexOutOfBoundsException();
  }
  return *CharArrayAddressOfElementAt(thiz, index);
}

KInt Kotlin_String_getStringLength(KString thiz) {
  return thiz->count_;
}

const char* unsafeByteArrayAsCString(KConstRef thiz, KInt start, KInt size) {
  const ArrayHeader* array = thiz->array();
  RuntimeAssert(array->type_info() == theByteArrayTypeInfo, "Must use a byte array");
  return reinterpret_cast<const char*>(ByteArrayAddressOfElementAt(array, start));
}

OBJ_GETTER(Kotlin_ByteArray_unsafeStringFromUtf8OrThrow, KConstRef thiz, KInt start, KInt size) {
  if (size == 0) {
    RETURN_RESULT_OF0(TheEmptyString);
  }
  const char* rawString = unsafeByteArrayAsCString(thiz, start, size);
  RETURN_RESULT_OF(utf8ToUtf16OrThrow, rawString, size);
}

OBJ_GETTER(Kotlin_ByteArray_unsafeStringFromUtf8, KConstRef thiz, KInt start, KInt size) {
  if (size == 0) {
    RETURN_RESULT_OF0(TheEmptyString);
  }
  const char* rawString = unsafeByteArrayAsCString(thiz, start, size);
  RETURN_RESULT_OF(utf8ToUtf16, rawString, size);
}

OBJ_GETTER(Kotlin_String_unsafeStringToUtf8, KString thiz, KInt start, KInt size) {
  RETURN_RESULT_OF(unsafeUtf16ToUtf8Impl<utf8::with_replacement::utf16to8>, thiz, start, size);
}

OBJ_GETTER(Kotlin_String_unsafeStringToUtf8OrThrow, KString thiz, KInt start, KInt size) {
  RETURN_RESULT_OF(unsafeUtf16ToUtf8Impl<utf16toUtf8OrThrow>, thiz, start, size);
}

KInt Kotlin_StringBuilder_insertString(KRef builder, KInt distIndex, KString fromString, KInt sourceIndex, KInt count) {
  auto toArray = builder->array();
  RuntimeAssert(sourceIndex >= 0 && static_cast<uint32_t>(sourceIndex + count) <= fromString->count_, "must be true");
  RuntimeAssert(distIndex >= 0 && static_cast<uint32_t>(distIndex + count) <= toArray->count_, "must be true");
  memcpy(CharArrayAddressOfElementAt(toArray, distIndex),
         CharArrayAddressOfElementAt(fromString, sourceIndex),
         count * sizeof(KChar));
  return count;
}

KInt Kotlin_StringBuilder_insertInt(KRef builder, KInt position, KInt value) {
  auto toArray = builder->array();
  RuntimeAssert(toArray->count_ >= static_cast<uint32_t>(11 + position), "must be true");
  char cstring[12];
  auto length = konan::snprintf(cstring, sizeof(cstring), "%d", value);
  RuntimeAssert(length >= 0, "This should never happen"); // may be overkill
  RuntimeAssert(static_cast<size_t>(length) < sizeof(cstring), "Unexpectedly large value"); // Can't be, but this is what sNprintf for
  auto* from = &cstring[0];
  auto* to = CharArrayAddressOfElementAt(toArray, position);
  while (*from) {
    *to++ = *from++;
  }
  return from - cstring;
}


KBoolean Kotlin_String_equals(KString thiz, KConstRef other) {
  if (other == nullptr || other->type_info() != theStringTypeInfo) return false;
  // Important, due to literal internalization.
  KString otherString = other->array();
  if (thiz == otherString) return true;
  return thiz->count_ == otherString->count_ &&
      memcmp(CharArrayAddressOfElementAt(thiz, 0),
             CharArrayAddressOfElementAt(otherString, 0),
             thiz->count_ * sizeof(KChar)) == 0;
}

KBoolean Kotlin_String_equalsIgnoreCase(KString thiz, KConstRef other) {
  RuntimeAssert(thiz->type_info() == theStringTypeInfo &&
                other->type_info() == theStringTypeInfo, "Must be strings");
  // Important, due to literal internalization.
  KString otherString = other->array();
  if (thiz == otherString) return true;
  if (thiz->count_ != otherString->count_) return false;
  auto count = thiz->count_;
  const KChar* thizRaw = CharArrayAddressOfElementAt(thiz, 0);
  const KChar* otherRaw = CharArrayAddressOfElementAt(otherString, 0);
  for (uint32_t index = 0; index < count; ++index) {
    if (towlower_Konan(*thizRaw++) != towlower_Konan(*otherRaw++)) return false;
  }
  return true;
}

KBoolean Kotlin_String_regionMatches(KString thiz, KInt thizOffset,
                                     KString other, KInt otherOffset,
                                     KInt length, KBoolean ignoreCase) {
  if (length < 0 ||
      thizOffset < 0 || length > static_cast<KInt>(thiz->count_) - thizOffset ||
      otherOffset < 0 || length > static_cast<KInt>(other->count_) - otherOffset) {
    return false;
  }
  const KChar* thizRaw = CharArrayAddressOfElementAt(thiz, thizOffset);
  const KChar* otherRaw = CharArrayAddressOfElementAt(other, otherOffset);
  if (ignoreCase) {
    for (KInt index = 0; index < length; ++index) {
      if (towlower_Konan(*thizRaw++) != towlower_Konan(*otherRaw++)) return false;
    }
  } else {
    for (KInt index = 0; index < length; ++index) {
      if (*thizRaw++ != *otherRaw++) return false;
    }
  }
  return true;
}

KBoolean Kotlin_Char_isIdentifierIgnorable(KChar ch) {
  RuntimeAssert(false, "Kotlin_Char_isIdentifierIgnorable() is not implemented");
  return false;
}

KBoolean Kotlin_Char_isISOControl(KChar ch) {
  return (ch <= 0x1F) || (ch >= 0x7F && ch <= 0x9F);
}

KBoolean Kotlin_Char_isHighSurrogate(KChar ch) {
  return ((ch & 0xfc00) == 0xd800);
}

KBoolean Kotlin_Char_isLowSurrogate(KChar ch) {
  return ((ch & 0xfc00) == 0xdc00);
}

KChar Kotlin_Char_toLowerCase(KChar ch) {
  return towlower_Konan(ch);
}

KChar Kotlin_Char_toUpperCase(KChar ch) {
  return towupper_Konan(ch);
}

constexpr KInt digits[] = {
  0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
  -1, -1, -1, -1, -1, -1, -1,
  10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
  20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
  30, 31, 32, 33, 34, 35,
  -1, -1, -1, -1, -1, -1,
  10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
  20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
  30, 31, 32, 33, 34, 35
};

// Based on Apache Harmony implementation.
// Radix check is performed on the Kotlin side.
KInt Kotlin_Char_digitOfChecked(KChar ch, KInt radix) {

  KInt result = -1;
  if (ch >= 0x30 /* 0 */ && ch <= 0x7a /* z */) {
    result = digits[ch - 0x30];
  } else {
    int index = -1;
    index = binarySearchRange(digitKeys, ARRAY_SIZE(digitKeys), ch);
    if (index >= 0 && ch <= digitValues[index * 2]) {
      result = ch - digitValues[index * 2 + 1];
    }
  }
  if (result >= radix) return -1;
  return result;
}

KInt Kotlin_String_indexOfChar(KString thiz, KChar ch, KInt fromIndex) {
  if (fromIndex < 0) {
    fromIndex = 0;
  }
  if (static_cast<uint32_t>(fromIndex) > thiz->count_) {
    return -1;
  }
  KInt count = thiz->count_;
  const KChar* thizRaw = CharArrayAddressOfElementAt(thiz, fromIndex);
  while (fromIndex < count) {
    if (*thizRaw++ == ch) return fromIndex;
    fromIndex++;
  }
  return -1;
}

KInt Kotlin_String_lastIndexOfChar(KString thiz, KChar ch, KInt fromIndex) {
  if (fromIndex < 0 || thiz->count_ == 0) {
    return -1;
  }
  if (static_cast<uint32_t>(fromIndex) >= thiz->count_) {
    fromIndex = thiz->count_ - 1;
  }
  KInt index = fromIndex;
  const KChar* thizRaw = CharArrayAddressOfElementAt(thiz, index);
  while (index >= 0) {
    if (*thizRaw-- == ch) return index;
    index--;
  }
  return -1;
}

// TODO: or code up Knuth-Moris-Pratt.
KInt Kotlin_String_indexOfString(KString thiz, KString other, KInt fromIndex) {
  if (fromIndex < 0) {
    fromIndex = 0;
  }
  if (static_cast<uint32_t>(fromIndex) >= thiz->count_) {
    return (other->count_ == 0) ? thiz->count_ : -1;
  }
  if (static_cast<KInt>(other->count_) > static_cast<KInt>(thiz->count_) - fromIndex) {
    return -1;
  }
  // An empty string can be always found.
  if (other->count_ == 0) {
    return fromIndex;
  }
  const KChar* thizRaw = CharArrayAddressOfElementAt(thiz, fromIndex);
  const KChar* otherRaw = CharArrayAddressOfElementAt(other, 0);
  void* result = konan::memmem(thizRaw, (thiz->count_ - fromIndex) * sizeof(KChar),
                               otherRaw, other->count_ * sizeof(KChar));
  if (result == nullptr) return -1;

  return (reinterpret_cast<intptr_t>(result) - reinterpret_cast<intptr_t>(
      CharArrayAddressOfElementAt(thiz, 0))) / sizeof(KChar);
}

KInt Kotlin_String_lastIndexOfString(KString thiz, KString other, KInt fromIndex) {
  KInt count = thiz->count_;
  KInt otherCount = other->count_;

  if (fromIndex < 0 || otherCount > count) {
    return -1;
  }
  if (otherCount == 0) {
    return fromIndex < count ? fromIndex : count;
  }

  KInt start = fromIndex;
  if (fromIndex > count - otherCount)
    start = count - otherCount;
  KChar firstChar = *CharArrayAddressOfElementAt(other, 0);
  while (true) {
    KInt candidate = Kotlin_String_lastIndexOfChar(thiz, firstChar, start);
    if (candidate == -1) return -1;
    KInt offsetThiz = candidate;
    KInt offsetOther = 0;
    while (++offsetOther < otherCount &&
           *CharArrayAddressOfElementAt(thiz, ++offsetThiz) ==
           *CharArrayAddressOfElementAt(other, offsetOther)) {}
    if (offsetOther == otherCount) {
      return candidate;
    }
    start = candidate - 1;
  }
}

KInt Kotlin_String_hashCode(KString thiz) {
  // TODO: consider caching strings hashes.
  return polyHash(thiz->count_, CharArrayAddressOfElementAt(thiz, 0));
}

const KChar* Kotlin_String_utf16pointer(KString message) {
  RuntimeAssert(message->type_info() == theStringTypeInfo, "Must use a string");
  const KChar* utf16 = CharArrayAddressOfElementAt(message, 0);
  return utf16;
}

KInt Kotlin_String_utf16length(KString message) {
  RuntimeAssert(message->type_info() == theStringTypeInfo, "Must use a string");
  return message->count_ * sizeof(KChar);
}


} // extern "C"
