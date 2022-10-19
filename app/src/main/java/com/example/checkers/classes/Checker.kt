package com.example.checkers.classes

import androidx.compose.ui.graphics.Color

class Checker(val color: CheckerColor, var selected: Boolean = false, var hidden: Boolean = false): ISquare {
    //var selected = false
    var isKing = false
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Checker

        if (color != other.color) return false
        if (selected != other.selected) return false
        if (isKing != other.isKing) return false

        return true
    }

    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + selected.hashCode()
        result = 31 * result + isKing.hashCode()
        return result
    }

}

enum class CheckerColor{
    BLACK, WHITE
}