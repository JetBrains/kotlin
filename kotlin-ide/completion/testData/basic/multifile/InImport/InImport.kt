package first

import second.<caret>

// EXIST: { itemText: "extensionFun", tailText: "() for String in second", attributes: "" }
// EXIST: { itemText: "extensionVal", tailText: " for Int in second", attributes: "" }
// EXIST: { itemText: "topLevelFun1", tailText: "(p: (String, Int) -> Unit) (second)", attributes: "" }
// EXIST: { itemText: "topLevelFun2", tailText: "(p: () -> Unit) (second)", attributes: "" }
// EXIST: { itemText: "topLevelVal", tailText: " (second)", attributes: "" }
// NOTHING_ELSE