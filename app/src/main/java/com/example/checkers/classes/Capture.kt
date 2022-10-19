package com.example.checkers.classes

class Capture(val takenCheckerPoint: Point, val finishPoint: Point, val startPoint: Point) {
    override fun toString(): String {
        return "Capture(takenCheckerPoint=$takenCheckerPoint, finishPoint=$finishPoint, startPoint=$startPoint)"
    }
}