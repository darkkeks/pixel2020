package darkkeks.pixel2020

import darkkeks.pixel2020.Colors.COLOR_TO_ID
import darkkeks.pixel2020.Colors.PALETTE
import java.awt.Color

object FlagType {
    const val place = 0
    const val bomb = 1
    const val freeze = 2
    const val pixel = 3
}

class Pixel(
    val x: Int,
    val y: Int,
    val color: Color,
    val flag: Int,
    val userId: Int = 0,
    val groupId: Int = 0
) {
    constructor(x: Int, y: Int, color: Color) : this(x, y, color, FlagType.pixel)
}

fun unpack(a: Int, b: Int, c: Int): Pixel {
    var value = a

    val x = a % FIELD_WIDTH
    value /= FIELD_WIDTH
    val y = value % FIELD_HEIGHT
    value /= FIELD_HEIGHT
    val color = value % PALETTE.size
    value /= PALETTE.size
    val flag = value

    return Pixel(x, y, Colors.RGB_MAP[color], flag, b, c)
}

fun Pixel.pack(): Int {
    val colorId = COLOR_TO_ID[color] ?: error("Invalid pixel color")
    return x + y * FIELD_WIDTH + PIXEL_COUNT * (colorId + flag * PALETTE.size)
}
