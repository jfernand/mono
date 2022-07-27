data class Decoded(val op: Op, val nextAddr: Int)

private fun simpleDecode(op: Op) = { addr: Int, _: Mem -> op at addr + 1 }
private fun memDecode(op: Op) = { addr: Int, mem: Mem -> op at addr + 2 }

infix fun Op.at(addr: Int) = Decoded(this, addr)

sealed class Op {
    init {
        println("Registering $this opCode $opCode")
        register()
    }

    companion object {
        val opCodes = mutableMapOf<Int, (Int, Mem) -> Decoded>()
    }

    abstract val opCode: Int
    abstract fun Machine.exec()

    abstract fun register()
    fun decode(addr: Int, mem: Mem): Decoded =
        (opCodes[mem[addr]] ?: error("Cannot decode instruction at $addr"))(addr, mem)

    open fun encode(addr: Int, mem: Mem) {
        mem[addr] = opCode
    }

    open val size = 1
}

object Noop : Op() {
    override val opCode: Int
        get() = 0

    override fun register() {
        opCodes[opCode] = { addr, _ -> Noop at addr + 1 }
    }

    override fun Machine.exec() {
    }

    override fun toString(): String = "NOP"

}

object Store : Op() {
    override val opCode: Int
        get() = 1

    init {
        println("Registering $this opCode $opCode")
        register()
    }

    override fun register() {
        opCodes[opCode] = simpleDecode(Store)
    }

    override fun Machine.exec() {
        val n1 = ds.pop()
        val addr = ds.pop()
        mem[addr] = n1
    }
    override fun toString(): String = "!"

}

object Fetch : Op() {
    override val opCode: Int
        get() = 2

    override fun register() {
        opCodes[opCode] = simpleDecode(this)
    }

    override fun Machine.exec() {
        val addr = ds.pop()
        ds.push(mem[addr])
    }

    override fun toString(): String = "@"
}

object Drop : Op() {
    override val opCode: Int = 3

    init {
        println("Registering $this opCode $opCode")
        register()
    }

    override fun register() {
        opCodes[opCode] = { addr, _ -> Drop at addr + 1 }
    }

    override fun Machine.exec() {
        ds.pop()
    }
    override fun toString(): String = "DROP"

}

object Dup : Op() {
    override val opCode: Int = 4

    init {
        println("Registering $this opCode $opCode")
        register()
    }

    override fun register() {
        opCodes[opCode] = { addr, _ -> Dup at addr + 1 }
    }

    override fun Machine.exec() {
        ds.push(ds.peek())
    }
    override fun toString(): String = "DUP"

}

object Push : Op() {
    override val opCode: Int = 5

    init {
        println("Registering $this opCode $opCode")
        register()
    }

    override fun register() {
        opCodes[opCode] = { addr, _ -> Push at addr + 1 }
    }

    override fun Machine.exec() {
        rs.push(ds.pop())
    }

    override fun toString(): String = ">R"

}

object Pop : Op() {
    override val opCode: Int
        get() = 6

    override fun register() {
        opCodes[opCode] = { addr, _ -> Pop at addr + 1 }
    }

    override fun Machine.exec() {
        ds.push(rs.pop())
    }
    override fun toString(): String = "R>"

}

object Swap : Op() {
    override val opCode: Int
        get() = 7

    override fun register() {
        opCodes[opCode] = { addr, _ -> Swap at addr + 1 }
    }

    override fun Machine.exec() {
        val n1 = ds.pop()
        val n2 = ds.pop()
        ds.push(n1)
        ds.push(n2)
    }

    override fun toString(): String = "SWAP"

}

data class Quot(private val n: Int) : Op() {
    override val opCode: Int
        get() = 8
    override val size = 2
    override fun register() {
        opCodes[opCode] = { addr, mem -> Quot(mem[addr + 1]) at addr + 2 }
    }

    override fun Machine.exec() {
        ds.push(n)
    }

    override fun encode(addr: Int, mem: Mem) {
        super.encode(addr, mem)
        mem[addr + 1] = n
    }

    override fun toString(): String = "'$n"
}

data class If(private val n: Int) : Op() {
    override val opCode: Int
        get() = 9

    override fun register() {
        opCodes[opCode] = { addr, mem -> If(mem[addr+1]) at addr + 2 }
    }

    override fun Machine.exec() {
        TODO("Not yet implemented")
    }
    override fun toString(): String = "IF $n"

}

class Call(private val n: Int) : Op() {
    override val opCode: Int
        get() = 10

    override fun register() {
        opCodes[opCode] = { addr, mem -> Call(mem[addr+1]) at addr + 2 }
    }

    override fun Machine.exec() {
        TODO("Not yet implemented")
    }
    override fun toString(): String = "CALL $n"

}

object Xor : Op() {
    override val opCode: Int
        get() = 11

    override fun register() {
        opCodes[opCode] = { addr, _ -> Xor at addr + 1 }
    }

    override fun Machine.exec() {
        val n1 = ds.pop()
        val n2 = ds.pop()
        ds.push(n1 xor n2)
    }
    override fun toString(): String = "XOR"

}

object Over : Op() {
    override val opCode: Int
        get() = 12

    override fun register() {
        opCodes[opCode] = { addr, _ -> Over at addr + 1 }
    }

    override fun Machine.exec() {
        TODO("Not yet implemented")
    }
    override fun toString(): String = "OVER"

}

object Exit : Op() {
    override val opCode: Int
        get() = 13

    override fun register() {
        opCodes[opCode] = { addr, _ -> Exit at addr + 1 }
    }

    override fun Machine.exec() {
        TODO("Not yet implemented")
    }
    override fun toString(): String = "EXIT"

}

object Minus : Op() {
    override val opCode: Int
        get() = 15

    override fun register() {
        opCodes[opCode] = { addr, _ -> Minus at addr + 1 }
    }

    override fun Machine.exec() {
        val n1 = ds.pop()
        val n2 = ds.pop()
        ds.push(n2 - n1)
    }

    override fun toString(): String = "-"

}

object Plus : Op() {
    override val opCode: Int
        get() = 14

    override fun register() {
        opCodes[opCode] = { addr, _ -> Plus at addr + 1 }
    }

    override fun Machine.exec() {
        ds.push(ds.pop() + ds.pop())
    }

    override fun toString(): String = "+"
}
