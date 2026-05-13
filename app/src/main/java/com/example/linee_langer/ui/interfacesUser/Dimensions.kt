package com.example.linee_langer.ui.interfacesUser

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp


object Spacing {
    val none = 0.dp
    val extraSmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val extraLarge = 32.dp
}

object Padding {

    val horizontalPadding = 12.dp
    val verticalPadding = 6.dp
    val superLargeTop = 80.dp
}

object Shape {
    val small = 16.dp
}

object Border {
    val superSmall = 1.dp
}

object IconSize {
    val superSmall = 14.dp
    val small = 18.dp
    val medium = 24.dp
    val large = 32.dp
}

object Button {
    val extraLargeHeight = 50.dp
}
object AppDimension {

    val Column = 200.dp
    // Spacing & Margins
    val None = 0.dp
    val One = 1.dp
    val Quirk = 2.dp
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 16.dp        // Standard padding
    val Large = 24.dp         // Section spacing
    val ExtraLarge = 32.dp    // Screen headers
    val Huge = 48.dp

    // Component Specific
    val ButtonHeight = 50.dp
    val ButtonWidth = 120.dp
    val IconSmall = 20.dp
    val IconMedium = 24.dp
    val IconLarge = 48.dp
    val IconSuper = 120.dp
    val CardCornerRadius = 16.dp
    val ButtonSize = 75.dp
    val Border = 3.dp

    val PaddingGeneral = 64.dp
    val PaddingTop = 48.dp
    val PaddingStart = 16.dp

    val PaddingBottom = 56.dp

    val CornerShape = 12.dp

    val VerticalPadding = 32.dp

    val HorizontalPadding = 20.dp

}

@Composable
fun rememberAppDimension(windowSize: WindowWidthSizeClass): AppDimension {
    return when (windowSize){
        WindowWidthSizeClass.Compact -> AppDimension
        else -> AppDimension
    }
}