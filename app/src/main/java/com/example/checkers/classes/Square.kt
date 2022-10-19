package com.example.checkers.classes

class Square :ISquare {

    var isMoveHighlighted = false
    var isCaptureHighlighted = false
    override fun toString(): String {
        return "Square(isMoveHighlighted=$isMoveHighlighted, isCaptureHighlighted=$isCaptureHighlighted)"
    }
}