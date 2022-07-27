import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.WindowConstants

fun main() {
    val turtle = makeWindowWithTurtle("Tortugas!")
    turtle.penDown()
    val nSides = 100
    repeat(nSides) {
        turtle.forward(900.0)
        turtle.right(122.50)
    }
//    flower()
}

fun flower() {
    val turtle = makeWindowWithTurtle("Tortugas!")
    turtle.penDown()
    repeat(24) {
        turtle.square()
        turtle.right(15.0)
    }
}

private fun Turtle.square() {
    repeat(4) {
        forward(400.0)
        right(90.0)
    }
}

private fun makeWindowWithTurtle(title: String): Turtle {
    val frame = JFrame(title)
    frame.isVisible = true
    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    val panel = Panel()
    frame.extendedState = JFrame.MAXIMIZED_BOTH
    frame.add(panel)
    println(frame)
    val size = java.awt.Toolkit.getDefaultToolkit().screenSize
    return Turtle(size.width / 4.0, size.height * 1.0,  -90.0,panel)
}

class Panel : JPanel() {
    var lines: Lines = mutableListOf()
    fun draw(line: Line) {
        Thread.sleep(10)
        lines.add(line)
        repaint()
    }

    override fun paint(g: java.awt.Graphics?) {
        g!!
        for (line in lines) {
            g.color = java.awt.Color(line.color.r, line.color.g, line.color.b)
            g.drawLine(line.from.x.toInt(), line.from.y.toInt(), line.to.x.toInt(), line.to.y.toInt())
        }
    }
}
