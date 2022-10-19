package com.example.checkers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.example.checkers.classes.Checker
import com.example.checkers.classes.CheckerColor
import com.example.checkers.classes.Game
import com.example.checkers.classes.Square
import com.example.checkers.ui.theme.CheckersTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val game = Game()

        setContent {
            CheckersTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xff333333)
                ) {
                    val configuration = LocalConfiguration.current
                    val screenWidth = configuration.screenWidthDp.dp
                    val squareSize = screenWidth / 8
                    val state = game.state.subscribeAsState()
                    Column {
                        gameUI(game, squareSize)
                        MoveTip(state, squareSize)
                    }


                }
            }
        }
    }

    @Composable
    private fun MoveTip(
        state: State<Game.State>,
        squareSize: Dp
    ) {
        Row(modifier = Modifier.padding(vertical = 10.dp)) {
            Text(text = "ХОД", modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(10.dp), color = Color.White)
            Image(
                bitmap = if (state.value.move == CheckerColor.WHITE)
                    ImageBitmap.imageResource(id = R.drawable.white_checker)
                else
                    ImageBitmap.imageResource(id = R.drawable.black_checker),
                contentDescription = null,
                modifier = Modifier
                    .height(squareSize * 0.8F)
                    .width(squareSize * 0.8F)
            )
        }
    }
}

@Composable
private fun gameUI(
    game: Game,
    squareSize: Dp
) {
    val state = game.state.subscribeAsState()

    Column {
        Box(
            Modifier
                .border(1.dp, Color.Black)
                .height(IntrinsicSize.Min)
        ) {
            Column {
                for ((i, row) in state.value.field.withIndex())
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(squareSize)
                    ) {
                        for ((j, square) in row.withIndex()) {
                            val color =
                                if ((i + j) % 2 == 0) Color(0xfffff685) else Color(
                                    0xFFB64000
                                )
                            Box(
                                modifier = Modifier
                                    .height(squareSize)
                                    .width(squareSize)
                                    .background(color)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { game.squareClicked(i, j) }
                                    )
                                //.border(1.dp, Color.Black),
                            ) {
                                when (square) {
                                    is Checker -> Checker(
                                        squareSize = squareSize,
                                        checker = square
                                    )
                                    is Square -> {
                                        if (square.isMoveHighlighted)
                                            Text(text = "MOVE")
                                        if (square.isCaptureHighlighted) {
                                            Text(text = "TAKE")
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
            
        }
    }
}

@Composable
private fun BoxScope.Checker(squareSize: Dp, checker: Checker) {

    val bitmap: ImageBitmap = if (checker.color == CheckerColor.WHITE){
        if (checker.isKing)
            ImageBitmap.imageResource(id = R.drawable.white_king)
        else
            ImageBitmap.imageResource(id = R.drawable.white_checker)
    }else{
        if (checker.isKing)
            ImageBitmap.imageResource(id = R.drawable.black_king)
        else
            ImageBitmap.imageResource(id = R.drawable.black_checker)
    }
    Image(
        bitmap = bitmap,
        contentDescription = null,

        modifier = Modifier
            .height(squareSize * 0.8F)
            .width(squareSize * 0.8F)
            .align(Alignment.Center)
            .border(if (checker.selected) 3.dp else 0.dp, Color.Cyan, CircleShape),
        alpha = if (checker.hidden) 0.5f else 1f
    )
}


fun blackChecker(): Checker {
    return Checker(CheckerColor.BLACK)
}

fun whiteChecker(): Checker {
    return Checker(CheckerColor.WHITE)
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CheckersTheme {
        gameUI(game = Game(), squareSize = 30.dp)
    }
}