package com.example.checkers.classes

class Point(val row: Int, val column: Int) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Point

        if (row != other.row) return false
        if (column != other.column) return false

        return true
    }

    override fun hashCode(): Int {
        var result = row
        result = 31 * result + column
        return result
    }

    override fun toString(): String {
        return "Point(row=$row, column=$column)"
    }
}