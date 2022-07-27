import Op.Companion.opCodes
import java.util.*

fun main(args: Array<String>) {
    val machine = machine {
        quot(3)
        quot(5)
        add()
        drop()
        fetch()
    }
    machine.encode()
    println(
        machine.mem
            .map { "%02x".format(it).uppercase() }
            .joinToString(" ")
            .chunked(24)
            .chunked(2)
            .map { it.joinToString("  ")}
            .joinToString("\n")
    )
    machine.decode()
}


interface IStack<E> {
    fun pop(): E
    fun push(op: E)
    fun peek(): E
}

class Stack<E> : IStack<E> {
    private val dq = ArrayDeque<E>()

    override fun pop(): E = dq.pop()
    override fun push(op: E) = dq.push(op)
    override fun peek() = dq.peek()
}

typealias Mem = Array<Int>

class Machine {
    val ops = mutableListOf<Op>()
    val ds = Stack<Int>()
    val rs = Stack<Int>()
    private val tos: Int
        get() = ds.peek()
    private val pc: Int = 0
    val mem: Mem = Array(128) {0}
    var start: Int = 0

    override fun toString(): String {
        return ops.toString()
    }
    fun start(n: Int) {
        start = n
    }

    fun encode() {
        var addr = start
        for( op in ops) {
            op.encode(addr, mem)
            addr += op.size
        }
    }
    fun decode() {
        var pc = start
        var op :Op= Noop
        while(op != Exit) {
            val opCode = mem[pc]
            val opDecoder = opCodes[opCode] ?: error("Unknown opcode $opCode at $pc")
            val (o, c) = opDecoder(pc, mem)
            println(o)
            op = o
            pc = c
        }
    }
    fun add() = ops.add(Plus)
    fun call(n: Int) = ops.add(Call(n))
    fun drop() = ops.add(Drop)
    fun dup() = ops.add(Dup)
    fun exit() = ops.add(Exit)
    fun fetch() = ops.add(Fetch)
    fun if_(n: Int) = ops.add(If(n))
    fun minus() = ops.add(Minus)
    fun over() = ops.add(Over)
    fun pop() = ops.add(Pop)
    fun push() = ops.add(Push)
    fun quot(n: Int) = ops.add(Quot(n))
    fun store(n: Int) = ops.add(Store)
    fun swap() = ops.add(Swap)
    fun xor() = ops.add(Xor)
    fun noop() = ops.add(Noop)
}

fun machine(init: Machine.() -> Unit): Machine {
    val machine = Machine()
    machine.init()
    machine.ops.add(Exit)
    return machine
}
