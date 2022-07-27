import org.crsh.cli.impl.invocation.InvocationMatch
import org.crsh.cli.impl.invocation.InvocationMatcher
import org.crsh.cli.impl.lang.Instance
import org.crsh.cli.impl.lang.Util
import org.crsh.standalone.CRaSH

fun main(args: Array<String>) {
    println("Hello World!")

    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    println("Program arguments: ${args.joinToString()}")
        val shell = CRaSH()
        val matcher: InvocationMatcher<Instance<CRaSH>> = shell.descriptor.matcher()
        val match: InvocationMatch<Instance<CRaSH>> = matcher.parse(line.toString())
        match.invoke(Util.wrap(main))
    }
}
