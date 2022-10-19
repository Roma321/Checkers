package com.example.checkers.classes

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.reduce
import com.example.checkers.blackChecker
import com.example.checkers.whiteChecker

class Game(private val size: Int = 8) {

    private val _state = MutableValue(
        State(
            field = initRussianField(),
            selectedPoint = null,
            move = CheckerColor.WHITE,
            availableMoves = emptyList(),
            availableCaptures = emptyList(),
            necessaryCaptures = emptyList(),
            inCaptureProcess = false
        )
    )
    val state: Value<State> = _state

    fun squareClicked(row: Int, column: Int) {
        if (state.value.field[row][column] is Checker) {
            if (!state.value.inCaptureProcess && (state.value.necessaryCaptures.isEmpty() || state.value.necessaryCaptures.map { it.startPoint }
                    .contains(
                        Point(row, column)
                    ))
            ) {//Не обрабатываем нажатия на шашки:
                // а) при начатом взятии
                // б)когда есть доступные взятия
                val newField: MutableList<MutableList<ISquare>> = copyField()
                val clickedChecker = newField[row][column] as Checker
                if (clickedChecker.color == state.value.move) {
                    when (state.value.selectedPoint) {
                        null -> {//Ничего не было выбрано - выделяем шашку
                            clickedChecker.selected = true
                            generateMoves(newField, row, column)
                        }
                        Point(row, column) -> { // Ткнули ту же самую - сбрасываем выделение
                            clickedChecker.selected = false
                            _state.reduce { it.copy(field = newField) }
                            _state.reduce { it.copy(selectedPoint = null) }
                            _state.reduce { it.copy(availableMoves = emptyList()) }
                        }
                        else -> {//Тык по другой шашке - сброс выделения старой и выделение новой
                            clickedChecker.selected = true
                            (newField[state.value.selectedPoint!!.row][state.value.selectedPoint!!.column] as Checker).selected =
                                false
                            generateMoves(newField, row, column)
                        }
                    }
                }
            }
        } else {
            println("Ну на пустую клетку нажали")
            val newField: MutableList<MutableList<ISquare>> = copyField()
            if (state.value.selectedPoint == null) return
            else {
                println("Знаем что есть выбранная шашка ${state.value.selectedPoint}")
                val lastSelectedChecker =
                    state.value.field[state.value.selectedPoint!!.row][state.value.selectedPoint!!.column] as Checker
                lastSelectedChecker.selected = false
                if (state.value.availableMoves.contains(Point(row, column))) {//доступный ход
                    newField[row][column] = lastSelectedChecker
                    newField[state.value.selectedPoint!!.row][state.value.selectedPoint!!.column] =
                        Square()
                    println("В avail moves")
                    changeColor()
                }
                for (capture in state.value.availableCaptures) {//Доступное взятие
                    println("Смотрим на $capture")
                    if (capture.finishPoint == Point(row, column)) {
                        println("1")
                        newField[row][column] = lastSelectedChecker
                        newField[state.value.selectedPoint!!.row][state.value.selectedPoint!!.column] =
                            Square()
                        (newField[capture.takenCheckerPoint.row][capture.takenCheckerPoint.column] as Checker).hidden =
                            true
                        val nowCheckerAt = Point(row, column)
                        if (getCaptures(
                                field = newField,
                                checkerAt = nowCheckerAt
                            ).isNotEmpty()
                        ) {
                            println("2")
                            _state.reduce { it.copy(inCaptureProcess = true) }
                            generateAndSaveCaptures(
                                newField, row, column
                            )
                            lastSelectedChecker.selected = true
                        } else {
                            changeColor()
                            println("3")
                            _state.reduce { it.copy(inCaptureProcess = false) }
                            clearHidden(field = newField)
                            break
                        }
                    }

                }
                _state.reduce { it.copy(availableMoves = emptyList()) }

                if (!state.value.inCaptureProcess) {
                    _state.reduce { it.copy(availableCaptures = emptyList()) }
                    _state.reduce { it.copy(selectedPoint = null) }
                }
                _state.reduce { it.copy(necessaryCaptures = emptyList()) }
                _state.reduce { it.copy(field = newField) }
                defineNecessaryCaptures()

            }

        }
    }

    private fun clearHidden(field: MutableList<MutableList<ISquare>>) {
        for ((i, listRow) in field.withIndex())
            for ((j, _) in listRow.withIndex()) {
                if (field[i][j] is Checker) {
                    if ((field[i][j] as Checker).hidden)
                        field[i][j] = Square()
                }
            }
    }

    private fun initRussianField(): MutableList<MutableList<ISquare>> {
        val field: MutableList<MutableList<ISquare>> =
            MutableList(8) { MutableList(8) { Square() } }

        field[0][1] = blackChecker()
        field[0][3] = blackChecker()
        field[0][5] = blackChecker()
        field[0][7] = blackChecker()
        field[1][0] = blackChecker()
        field[1][2] = blackChecker()
        field[1][4] = blackChecker()
//        field[1][4] = Checker(color = CheckerColor.WHITE)
//        (field[1][4] as Checker).isKing = true
        field[1][6] = blackChecker()
        field[2][1] = blackChecker()
        field[2][3] = blackChecker()
        field[2][5] = blackChecker()
        field[2][7] = blackChecker()
//        field[2][6] = Checker(CheckerColor.WHITE)
//        (field[2][6] as Checker).selected = true

        field[5][0] = whiteChecker()
        field[5][2] = whiteChecker()
        field[5][4] = whiteChecker()
        field[5][6] = whiteChecker()
        field[6][1] = whiteChecker()
        field[6][3] = whiteChecker()
        field[6][5] = whiteChecker()
        field[6][7] = whiteChecker()
        field[7][0] = whiteChecker()
        field[7][2] = whiteChecker()
        field[7][4] = whiteChecker()
        field[7][6] = whiteChecker()
        return field
    }

    /**
     * Сохраняет в состояние и подсвечивает доступные ходы (взятия) для шашки с учётом наличия взятий
     */
    private fun generateMoves(
        newField: MutableList<MutableList<ISquare>>,
        row: Int,
        column: Int
    ) {
        val captures = generateAndSaveCaptures(newField, row, column)

        if (captures.isEmpty()) {
            val moves = getMovesForChecker(
                field = newField,
                checkerAt = Point(row, column)
            )
            for (move in moves) {
                (newField[move.row][move.column] as Square).isMoveHighlighted = true
            }
            _state.reduce { it.copy(availableMoves = moves) }
        }

    }

    private fun generateAndSaveCaptures(
        newField: MutableList<MutableList<ISquare>>,
        row: Int,
        column: Int
    ): List<Capture> {
        val captures = getCaptures(
            field = newField,
            checkerAt = Point(row, column)
        )
        for (capture in captures) {
            (newField[capture.finishPoint.row][capture.finishPoint.column] as Square).isCaptureHighlighted =
                true
        }
        _state.reduce { it.copy(field = newField) }
        _state.reduce { it.copy(selectedPoint = Point(row, column)) }
        _state.reduce { it.copy(availableCaptures = captures) }
        return captures
    }

    private fun copyField(): MutableList<MutableList<ISquare>> {
        val newField: MutableList<MutableList<ISquare>> =
            MutableList(size) { MutableList(size) { Square() } }
        for ((i, listRow) in state.value.field.withIndex())
            for ((j, _) in listRow.withIndex()) {
                newField[i][j] = state.value.field[i][j]
                if (newField[i][j] is Square) {
                    (newField[i][j] as Square).isMoveHighlighted = false
                    (newField[i][j] as Square).isCaptureHighlighted = false
                }
            }
        return newField
    }

    private fun changeColor() {
        if (state.value.move == CheckerColor.WHITE)
            _state.reduce { it.copy(move = CheckerColor.BLACK) }
        else _state.reduce { it.copy(move = CheckerColor.WHITE) }
    }

    data class State(
        val field: MutableList<MutableList<ISquare>>,
        val selectedPoint: Point?,
        val move: CheckerColor,
        val availableMoves: List<Point>,
        val availableCaptures: List<Capture>,
        val necessaryCaptures: List<Capture>,
        val inCaptureProcess: Boolean,
    ) {
        override fun toString(): String {
            return "State(selectedPoint=$selectedPoint, move=$move, availableMoves=$availableMoves, availableCaptures=$availableCaptures, necessaryCaptures=$necessaryCaptures, inCaptureProcess=$inCaptureProcess)"
        }
    }


    private fun getMovesForChecker(
        field: MutableList<MutableList<ISquare>>,
        checkerAt: Point
    ): List<Point> {
        // println(state.value.move)
        return if (state.value.move == CheckerColor.WHITE)
            getMovesForWhiteChecker(field, checkerAt)
        else getMovesForBlackChecker(field, checkerAt)
    }

    private fun getMovesForBlackChecker(
        field: MutableList<MutableList<ISquare>>,
        checkerAt: Point
    ): List<Point> {
        val res = mutableListOf<Point>()
        try {
            if (field[checkerAt.row + 1][checkerAt.column + 1] !is Checker)
                res.add(Point(checkerAt.row + 1, checkerAt.column + 1))
        } catch (_: Exception) {
        }
        try {
            if (field[checkerAt.row + 1][checkerAt.column - 1] !is Checker)
                res.add(Point(checkerAt.row + 1, checkerAt.column - 1))
        } catch (_: Exception) {
        }

        return res
    }

    private fun getMovesForWhiteChecker(
        field: MutableList<MutableList<ISquare>>,
        checkerAt: Point
    ): List<Point> {
        val res = mutableListOf<Point>()
        try {
            if (field[checkerAt.row - 1][checkerAt.column + 1] !is Checker)
                res.add(Point(checkerAt.row - 1, checkerAt.column + 1))
        } catch (_: Exception) {
        }
        try {
            if (field[checkerAt.row - 1][checkerAt.column - 1] !is Checker)
                res.add(Point(checkerAt.row - 1, checkerAt.column - 1))
        } catch (_: Exception) {
        }

        return res
    }


    private fun getCaptures(
        field: MutableList<MutableList<ISquare>>,
        checkerAt: Point,
    ): List<Capture> {
        val res = mutableListOf<Capture>()
        if ((field[checkerAt.row][checkerAt.column] as Checker).isKing) {
            when (checkerAt.row) {
                //Взятия только назад
                in 0..1 -> {
                    addBackCapturesForKing(checkerAt, field, res)
                }
                in 1..size - 3 -> {
                    addBackCapturesForKing(checkerAt, field, res)
                    addForwardCapturesForKing(checkerAt, field, res)
                }
                //Взятия только вперёд
                in size - 2 until size -> {
                    addForwardCapturesForKing(checkerAt, field, res)
                }
                else -> throw IllegalArgumentException("Не та строка")
            }
        } else {
            when (checkerAt.row) {
                //Взятия только назад
                in 0..1 -> {
                    addBackCapturesForChecker(checkerAt, field, res)
                }
                in 1..size - 3 -> {
                    addBackCapturesForChecker(checkerAt, field, res)
                    addForwardCapturesForChecker(checkerAt, field, res)
                }
                //Взятия только вперёд
                in size - 2 until size -> {
                    addForwardCapturesForChecker(checkerAt, field, res)
                }
                else -> throw IllegalArgumentException("Не та строка")
            }
        }
        return res
    }

    private fun addForwardCapturesForKing(
        checkerAt: Point,
        field: MutableList<MutableList<ISquare>>,
        res: MutableList<Capture>
    ) {
        var i = checkerAt.row - 1
        var j = checkerAt.column + 1
        if (checkerAt.column < size - 2) {
            while (i > 0 && j < size - 1) {
                //Пустая клетка - смотрим дальше
                if (field[i][j] !is Checker) {
                    i--
                    j++
                    continue
                }
                //Встретили свою - брать нечего
                if (field[i][j] is Checker && (field[i][j] as Checker).color == state.value.move) break
                //Утыкаемся в уже съеденную - значит всё
                if (field[i][j] is Checker && (field[i][j] as Checker).hidden) break
                //встретили чужую - добавляем свободные поля
                if (field[i][j] is Checker && (field[i][j] as Checker).color != state.value.move) {
                    val finishes = freeSquaresForwardRight(field, i, j)
                    res.addAll(finishes.map {
                        Capture(
                            takenCheckerPoint = Point(i, j),
                            startPoint = checkerAt,
                            finishPoint = it
                        )
                    })
                    break
                }
                i--
                j++
            }
        }

        i = checkerAt.row - 1
        j = checkerAt.column - 1
        if (checkerAt.column > 1) {
            while (i > 0 && j > 0) {
                //Пустая клетка - смотрим дальше
                if (field[i][j] !is Checker) {
                    i--
                    j--
                    continue
                }
                //Встретили свою - брать нечего
                if (field[i][j] is Checker && (field[i][j] as Checker).color == state.value.move) break
                //Утыкаемся в уже съеденную - значит всё
                if (field[i][j] is Checker && (field[i][j] as Checker).hidden) break
                //встретили чужую - добавляем свободные поля
                if (field[i][j] is Checker && (field[i][j] as Checker).color != state.value.move) {
                    val finishes = freeSquaresForwardLeft(field, i, j)
                    res.addAll(finishes.map {
                        Capture(
                            takenCheckerPoint = Point(i, j),
                            startPoint = checkerAt,
                            finishPoint = it
                        )
                    })
                    break
                }
                i--
                j--
            }
        }
    }

    private fun freeSquaresForwardLeft(
        field: MutableList<MutableList<ISquare>>,
        fromRow: Int,
        fromColumn: Int
    ): List<Point> {
        var i = fromRow - 1
        var j = fromColumn - 1
        val list = mutableListOf<Point>()
        while (i >= 0 && j >= 0) {
            if (field[i][j] is Square)
                list.add(Point(i, j))
            else break
            i--
            j--
        }
        return list
    }

    private fun freeSquaresForwardRight(
        field: MutableList<MutableList<ISquare>>,
        fromRow: Int,
        fromColumn: Int
    ): List<Point> {
        var i = fromRow - 1
        var j = fromColumn + 1
        val list = mutableListOf<Point>()
        while (i >= 0 && j < size) {
            if (field[i][j] is Square)
                list.add(Point(i, j))
            else break
            i--
            j++
        }
        return list
    }

    private fun addBackCapturesForKing(
        checkerAt: Point,
        field: MutableList<MutableList<ISquare>>,
        res: MutableList<Capture>
    ) {
        var i = checkerAt.row + 1
        var j = checkerAt.column + 1
        if (checkerAt.column < size - 2) {
            while (i < size - 1 && j < size - 1) {
                //Пустая клетка - смотрим дальше
                if (field[i][j] !is Checker) {
                    i++
                    j++
                    continue
                }
                //Встретили свою - брать нечего
                if (field[i][j] is Checker && (field[i][j] as Checker).color == state.value.move) break
                //Утыкаемся в уже съеденную - значит всё
                if (field[i][j] is Checker && (field[i][j] as Checker).hidden) break
                //встретили чужую - добавляем свободные поля
                if (field[i][j] is Checker && (field[i][j] as Checker).color != state.value.move) {
                    val finishes = freeSquaresBackRight(field, i, j)
                    res.addAll(finishes.map {
                        Capture(
                            takenCheckerPoint = Point(i, j),
                            startPoint = checkerAt,
                            finishPoint = it
                        )
                    })
                    break
                }
                i++
                j++
            }
        }

        i = checkerAt.row + 1
        j = checkerAt.column - 1
        if (checkerAt.column > 1) {
            while (i < size - 1 && j > 0) {
                //Пустая клетка - смотрим дальше
                if (field[i][j] !is Checker) {
                    i++
                    j--
                    continue
                }
                //Встретили свою - брать нечего
                if (field[i][j] is Checker && (field[i][j] as Checker).color == state.value.move) break
                //Утыкаемся в уже съеденную - значит всё
                if (field[i][j] is Checker && (field[i][j] as Checker).hidden) break
                //встретили чужую - добавляем свободные поля
                if (field[i][j] is Checker && (field[i][j] as Checker).color != state.value.move) {
                    val finishes = freeSquaresBackLeft(field, i, j)
                    res.addAll(finishes.map {
                        Capture(
                            takenCheckerPoint = Point(i, j),
                            startPoint = checkerAt,
                            finishPoint = it
                        )
                    })
                    break
                }
                i++
                j--
            }
        }
    }

    private fun freeSquaresBackLeft(
        field: MutableList<MutableList<ISquare>>, fromRow: Int,
        fromColumn: Int
    ): List<Point> {
        var i = fromRow + 1
        var j = fromColumn - 1
        val list = mutableListOf<Point>()
        while (i < size && j >= 0) {
            if (field[i][j] is Square)
                list.add(Point(i, j))
            else break
            i++
            j--
        }
        return list
    }

    private fun freeSquaresBackRight(
        field: MutableList<MutableList<ISquare>>, fromRow: Int,
        fromColumn: Int
    ): List<Point> {
        var i = fromRow + 1
        var j = fromColumn + 1
        val list = mutableListOf<Point>()
        while (i < size && j < size) {
            if (field[i][j] is Square)
                list.add(Point(i, j))
            else break
            i++
            j++
        }
        return list
    }

    private fun addForwardCapturesForChecker(
        checkerAt: Point,
        field: MutableList<MutableList<ISquare>>,
        res: MutableList<Capture>
    ) {
        if (checkerAt.column < size - 2) {//ищем взятие вправо
            if (field[checkerAt.row - 2][checkerAt.column + 2] !is Checker
                && field[checkerAt.row - 1][checkerAt.column + 1] is Checker
                && (field[checkerAt.row - 1][checkerAt.column + 1] as Checker).color != state.value.move
                && !(field[checkerAt.row - 1][checkerAt.column + 1] as Checker).hidden
            )
                res.add(
                    Capture(
                        takenCheckerPoint = Point(
                            checkerAt.row - 1,
                            checkerAt.column + 1
                        ),
                        finishPoint = Point(
                            checkerAt.row - 2,
                            checkerAt.column + 2
                        ),
                        startPoint = checkerAt
                    )
                )
        }

        if (checkerAt.column > 1) {//ищем взятие влево
            if (field[checkerAt.row - 2][checkerAt.column - 2] !is Checker
                && field[checkerAt.row - 1][checkerAt.column - 1] is Checker
                && (field[checkerAt.row - 1][checkerAt.column - 1] as Checker).color != state.value.move
                && !(field[checkerAt.row - 1][checkerAt.column - 1] as Checker).hidden
            )
                res.add(
                    Capture(
                        takenCheckerPoint = Point(
                            checkerAt.row - 1,
                            checkerAt.column - 1
                        ),
                        finishPoint = Point(
                            checkerAt.row - 2,
                            checkerAt.column - 2
                        ),
                        startPoint = checkerAt
                    )
                )
        }
    }

    private fun defineNecessaryCaptures() {
        for ((i, listRow) in state.value.field.withIndex())
            for ((j, square) in listRow.withIndex()) {
                if (square is Checker) {
                    if (square.color == state.value.move) {
                        val captures = getCaptures(state.value.field, Point(i, j))
                        if (captures.isNotEmpty())
                            _state.reduce {
                                it.copy(
                                    necessaryCaptures = it.necessaryCaptures.plus(
                                        captures
                                    )
                                )
                            }
                    }

                }
            }
        println(state.value.necessaryCaptures)
    }

    private fun addBackCapturesForChecker(
        checkerAt: Point,
        field: MutableList<MutableList<ISquare>>,
        res: MutableList<Capture>
    ) {
        if (checkerAt.column < size - 2) {//ищем взятие вправо
            if (field[checkerAt.row + 2][checkerAt.column + 2] !is Checker
                && field[checkerAt.row + 1][checkerAt.column + 1] is Checker
                && (field[checkerAt.row + 1][checkerAt.column + 1] as Checker).color != state.value.move
                && !(field[checkerAt.row + 1][checkerAt.column + 1] as Checker).hidden
            )
                res.add(
                    Capture(
                        takenCheckerPoint = Point(
                            checkerAt.row + 1,
                            checkerAt.column + 1
                        ),
                        finishPoint = Point(
                            checkerAt.row + 2,
                            checkerAt.column + 2
                        ),
                        startPoint = checkerAt
                    )
                )
        }

        if (checkerAt.column > 1) {//ищем взятие влево
            if (field[checkerAt.row + 2][checkerAt.column - 2] !is Checker
                && field[checkerAt.row + 1][checkerAt.column - 1] is Checker
                && (field[checkerAt.row + 1][checkerAt.column - 1] as Checker).color != state.value.move
                && !(field[checkerAt.row + 1][checkerAt.column - 1] as Checker).hidden
            )
                res.add(
                    Capture(
                        takenCheckerPoint = Point(
                            checkerAt.row + 1,
                            checkerAt.column - 1
                        ),
                        finishPoint = Point(
                            checkerAt.row + 2,
                            checkerAt.column - 2
                        ),
                        startPoint = checkerAt
                    )
                )
        }
    }

}