data class Point(val x: Double, val y: Double)
data class Color(val r: Int, val g: Int, val b: Int)

val BLACK = Color(0, 0, 0)
val WHITE = Color(255, 255, 255)

data class Line(val from: Point, val to: Point, val color: Color)

fun Line.toList(): Lines = mutableListOf(this)
//fun Lines.addAll(lines :Lines) = addAll(lines)

typealias Lines = MutableList<Line>

open class Turtle(var x: Double, var y: Double, var angle: Double, open val panel: Panel) {
    protected var drawing = true

    open fun penDown() {
        drawing = true
    }

    open fun penUp() {
        drawing = false
    }

    open fun showPos(): Point {
        return Point(x, y)
    }

    protected fun move(len: Double) {
        val from = Point(x, y)
        x += len * Math.cos(angle * Math.PI / 180)
        y += len * Math.sin(angle * Math.PI / 180)
        panel.draw(Line(from, Point(x, y), BLACK))
    }

    open fun forward(len: Double) {
        val p1 = showPos()
        move(len)
        val p2 = showPos()
        if (drawing) {
            println("{ \"from\": $p1, \"to\": $p2 }")
            return panel.draw(Line(p1, p2, BLACK))
        }
        return panel.draw(Line(p1, p1, BLACK))
    }

    open fun right(a: Double) {
        angle += a
    }

    open fun left(a: Double) {
        angle -= a
    }

    open fun triangle(len: Double) {
        forward(len); right(120.0); forward(len); right(120.0); forward(len); right(120.0);
    }
}
