import java.lang.Math.random

fun range360(h0: Double): Double {
    var h = h0
    while (h >= 360) h -= 360
    while (h < 0) h += 360
    return h
}

fun hue360(h0: Double): Triple<Double, Double, Double> {
    val h = range360(h0)
    val h1: Int = kotlin.math.floor(h).toInt() / 60
    val f = h / 60 - h1

    return when (h1) {
        0 -> Triple(1.0, f, 0.0)
        1 -> Triple(1 - f, 1.0, 0.0)
        2 -> Triple(0.0, 1.0, f)
        3 -> Triple(0.0, 1 - f, 1.0)
        4 -> Triple(f, 0.0, 1.0)
        else -> Triple(1.0, 0.0, 1 - f)
    }
}

fun showColor(color: Int): String {
    return "%02x%02x%02x".format(color / 0x10000 % 0x100, color / 0x100 % 0x100, color % 0x100)
}

fun range1(r: Double): Double {
    return if (r < 0.0) 0.0 else if (r > 1.0) 1.0 else r
}

fun showColor(r0: Double, g0: Double, b0: Double): String {
    return "#%02x%02x%02x".format((r0 * 255).toInt(), (g0 * 255).toInt(), (b0 * 255).toInt())
}

open class ColorTurtle(x: Double, y: Double, angle: Double, var color: Double, override val panel: Panel) :
    Turtle(x, y, angle, panel) {
    open fun showColor(): Color {
        val (r, g, b) = hue360(color)
//        return showColor(r, g, b)
        return Color((255 * r).toInt(), (255 * g).toInt(), (255 * b).toInt())
    }

    open fun rotateColor(angle: Double) {
        color += angle
    }

    override fun forward(len: Double) {
        val p1 = showPos()
        move(len)
        val p2 = showPos()
        val c = showColor()
        if (drawing) {
            println("{ \"color\": \"$c\", \"from\": $p1, \"to\": $p2 }")
            panel.draw(Line(p1, p2, c))
        }
        panel.draw(Line(p1, p1, c))
    }
}

class HalfTurtle(x: Double, y: Double, angle: Double, override val panel: Panel) : Turtle(x, y, angle, panel) {
    override fun right(a: Double) {
        super.right(a / 2)
    }
}

class PerverseTurtle(x: Double, y: Double, angle: Double, override val panel: Panel) : Turtle(x, y, -angle, panel) {
    override fun right(a: Double) {
        super.right(-a)
    }
}

class DrunkTurtle(x: Double, y: Double, angle: Double, override val panel: Panel) : Turtle(x, y, angle, panel) {
    override fun forward(len: Double){
        val p1 = showPos()
        move(len)
        x += len * (random() * 0.1 - 0.05)
        y += len * (random() * 0.1 - 0.05)
        val p2 = showPos()
        if (drawing) {
            println("{ \"from\": $p1, \"to\": $p2 }")
            panel.draw(Line(p1, p2, BLACK))
        }
        panel.draw(Line(p1, p1, BLACK))
    }
}

class ZigZagTurtle(x: Double, y: Double, angle: Double, override val panel:Panel) : Turtle(x, y, angle, panel) {
    override fun forward(len: Double) {
        val d = len / 4
        right(60.0)
        (super.forward(d))
        right(-120.0)
        (super.forward(2 * d))
        right(120.0)
        (super.forward(2 * d))
        right(-120.0)
        (super.forward(2 * d))
        right(120.0)
        (super.forward(d))
        right(-60.0)
    }
}

class RainbowTurtle(x: Double, y: Double, angle: Double, color: Double, override val panel:Panel) : ColorTurtle(x, y, angle, color, panel) {
    var rest = 0.0

    override fun forward(len0: Double) {
        var len = len0

        if (rest > 0.0) {
            if (len >= rest) {
                (super.forward(rest))
                rotateColor(12.0)
                len -= rest
                rest = 0.0
            } else {
                (super.forward(len))
                rest -= len
                /* rest != 0.0 */
            }
        }
        /* rest == 0.0 */
        while (len >= 10) {
            (super.forward(10.0))
            rotateColor(12.0)
            len -= 10
        }
        if (len > 0) {
            (super.forward(len))
            rest = 10 - len
        }
    }
}

fun main(args: Array<String>) {
//    val turtles = listOf(
//        ZigZagTurtle(100.0, 500.0, 0.0),
//        DrunkTurtle(250.0, 500.0, 0.0),
//        RainbowTurtle(400.0, 500.0, 0.0, 0.0))
//
//    for (i in 1..5) {
//        for (turtle in turtles) {
//            turtle.forward(100.0)
//            turtle.triangle(10.0)
//            turtle.right(144.0)
//        }
//    }
}
