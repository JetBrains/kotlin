package model

import annotation.ProcessThis

@ProcessThis
interface Model {

    var a: Int

    var b: Int
}

@ProcessThis
class Class {

    var a = 0
}