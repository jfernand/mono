/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

import org.crsh.cli.Argument
import org.crsh.cli.Command
import org.crsh.cli.Named
import org.crsh.cli.Option
import org.crsh.cli.Usage
import org.crsh.cli.descriptor.CommandDescriptor
import org.crsh.cli.impl.Delimiter
import org.crsh.cli.impl.lang.CommandFactory
import org.crsh.cli.impl.lang.Instance
import org.crsh.cli.impl.lang.Util
import org.crsh.console.jline.JLineProcessor
import org.crsh.console.jline.TerminalFactory
import org.crsh.console.jline.console.ConsoleReader
import org.crsh.console.jline.internal.Configuration
import org.crsh.plugin.ResourceManager
import org.crsh.shell.Shell
import org.crsh.shell.ShellFactory
import org.crsh.shell.impl.remoting.RemoteServer
import org.crsh.standalone.Agent
import org.crsh.standalone.Bootstrap
import org.crsh.util.CloseableList
import org.crsh.util.InterruptHandler
import org.crsh.util.Utils
import org.crsh.vfs.FS
import org.crsh.vfs.File
import org.crsh.vfs.Path
import org.crsh.vfs.spi.file.FileMountFactory
import org.crsh.vfs.spi.url.ClassPathMountFactory
import org.fusesource.jansi.AnsiConsole
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintStream
import java.util.*
import java.util.jar.*
import java.util.logging.*
import java.util.regex.*

@Named("crash")
class Shelll {
    /** .  */
    private val descriptor: CommandDescriptor<Instance<Shelll>>

    init {
        descriptor = CommandFactory.DEFAULT.create(Shelll::class.java)
    }

    @Throws(IOException::class)
    private fun copyCmd(src: File, dst: java.io.File) {
        if (src.hasChildren()) {
            if (!dst.exists()) {
                if (dst.mkdir()) {
                    log.fine("Could not create dir " + dst.canonicalPath)
                }
            }
            if (dst.exists() && dst.isDirectory) {
                for (child in src.children()) {
                    copyCmd(child, java.io.File(dst, child.name))
                }
            }
        } else {
            if (!dst.exists()) {
                val resource = src.resource
                if (resource != null) {
                    log.info("Copied command " + src.path.value + " to " + dst.canonicalPath)
                    Utils.copy(ByteArrayInputStream(resource.content), FileOutputStream(dst))
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun copyConf(src: File, dst: java.io.File) {
        if (!src.hasChildren()) {
            if (!dst.exists()) {
                val resource = ResourceManager.loadConf(src)
                if (resource != null) {
                    log.info("Copied resource " + src.path.value + " to " + dst.canonicalPath)
                    Utils.copy(ByteArrayInputStream(resource.content), FileOutputStream(dst))
                }
            }
        }
    }

    private fun toString(builder: FS.Builder): String {
        val sb = StringBuilder()
        val mounts = builder.mounts
        for (i in mounts.indices) {
            val mount = mounts[i]
            if (i > 0) {
                sb.append(';')
            }
            sb.append(mount.value)
        }
        return sb.toString()
    }

    @Throws(IOException::class)
    private fun createBuilder(): FS.Builder {
        val fileDriver = FileMountFactory(Utils.getCurrentDirectory())
        val classpathDriver = ClassPathMountFactory(Thread.currentThread().contextClassLoader)
        return FS.Builder().register("file", fileDriver).register("classpath", classpathDriver)
    }

    @Command
    @Throws(Exception::class)
    fun main(
        @Option(names = ["non-interactive"]) @Usage("non interactive mode, the JVM io will not be used") nonInteractive: Boolean?,
        @Option(names = ["c", "cmd"]) @Usage("the command mounts") cmd: String?,
        @Option(names = ["conf"]) @Usage("the conf mounts") conf: String?,
        @Option(names = ["p", "property"]) @Usage("set a property of the form a=b") properties: List<String>?,
        @Option(names = ["cmd-folder"]) @Usage("a folder in which commands should be extracted") cmdFolder: String?,
        @Option(names = ["conf-folder"]) @Usage("a folder in which configuration should be extracted") confFolder: String?,
        @Argument(name = "pid") @Usage("the optional list of JVM process id to attach to") pids: List<Int>?,
    ) {

        //
        var cmd = cmd
        var conf = conf
        val interactive = nonInteractive == null || !nonInteractive

        //
        if (conf == null) {
            conf = "classpath:/crash/"
        }
        var confBuilder = createBuilder().mount(conf)
        if (confFolder != null) {
            val dst = java.io.File(confFolder)
            if (!dst.isDirectory) {
                throw Exception("Directory " + dst.absolutePath + " does not exist")
            }
            val f = confBuilder.build()[Path.get("/")]
            log.info("Extracting conf resources to " + dst.absolutePath)
            for (child in f.children()) {
                if (!child.hasChildren()) {
                    copyConf(child, java.io.File(dst, child.name))
                }
            }
            confBuilder = createBuilder().mount("file", Path.get(dst))
        }

        //
        if (cmd == null) {
            cmd = "classpath:/crash/commands/"
        }
        var cmdBuilder = createBuilder().mount(cmd)
        if (cmdFolder != null) {
            val dst = java.io.File(cmdFolder)
            if (!dst.isDirectory) {
                throw Exception("Directory " + dst.absolutePath + " does not exist")
            }
            val f = cmdBuilder.build()[Path.get("/")]
            log.info("Extracting command resources to " + dst.absolutePath)
            copyCmd(f, dst)
            cmdBuilder = createBuilder().mount("file", Path.get(dst))
        }

        //
        log.log(Level.INFO, "conf mounts: $confBuilder")
        log.log(Level.INFO, "cmd mounts: $cmdBuilder")

        //
        var closeable: CloseableList? = CloseableList()
        val shell: Shell?
        if (pids != null && pids.size > 0) {

            //
            if (interactive && pids.size > 1) {
                throw Exception("Cannot attach to more than one JVM in interactive mode")
            }

            // Compute classpath
            val classpath = System.getProperty("java.class.path")
            val sep = System.getProperty("path.separator")
            val buffer = StringBuilder()
            for (path in classpath.split(Pattern.quote(sep)).toTypedArray()) {
                val file = java.io.File(path)
                if (file.exists()) {
                    if (buffer.length > 0) {
                        buffer.append(' ')
                    }
                    var fileName = file.canonicalPath
                    if (fileName[0] != '/' && fileName[1] == ':') {
                        // On window, the value of Class-Path in Manifest file must in form: /C:/path/lib/abc.jar
                        fileName = fileName.replace(java.io.File.separatorChar, '/')
                        buffer.append("/").append(fileName)
                    } else {
                        buffer.append(file.canonicalPath)
                    }
                }
            }

            // Create manifest
            val manifest = Manifest()
            val attributes = manifest.mainAttributes
            attributes.putValue("Agent-Class", Agent::class.java.name)
            attributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
            attributes[Attributes.Name.CLASS_PATH] = buffer.toString()

            // Create jar file
            val agentFile = java.io.File.createTempFile("agent", ".jar")
            agentFile.deleteOnExit()
            val out = JarOutputStream(FileOutputStream(agentFile), manifest)
            out.close()
            log.log(Level.INFO, "Created agent jar " + agentFile.canonicalPath)

            // Build the options
            val sb = StringBuilder()

            // Path configuration
            sb.append("--cmd ")
            Delimiter.EMPTY.escape(toString(cmdBuilder), sb)
            sb.append(' ')
            sb.append("--conf ")
            Delimiter.EMPTY.escape(toString(confBuilder), sb)
            sb.append(' ')

            // Propagate canonical config
            if (properties != null) {
                for (property in properties) {
                    sb.append("--property ")
                    Delimiter.EMPTY.escape(property, sb)
                    sb.append(' ')
                }
            }

            //
            if (interactive) {
                val server = RemoteServer(0)
                val port = server.bind()
                log.log(Level.INFO,
                    "Callback server set on port $port")
                sb.append(port)
                val options = sb.toString()
                val pid = pids[0]
                log.log(Level.INFO, "Loading agent with command " + options + " as agent " + agentFile.canonicalPath)
                server.accept()
                shell = server.shell
            } else {
                for (pid in pids) {
                    log.log(Level.INFO,
                        "Attaching to remote process $pid")
                    val options = sb.toString()
                    log.log(Level.INFO,
                        "Loading agent with command " + options + " as agent " + agentFile.canonicalPath)
                }
                shell = null
            }
        } else {
            val bootstrap = Bootstrap(
                Thread.currentThread().contextClassLoader,
                confBuilder.build(),
                cmdBuilder.build())

            //
            if (properties != null) {
                val config = Properties()
                for (property in properties) {
                    val index = property.indexOf('=')
                    if (index == -1) {
                        config.setProperty(property, "")
                    } else {
                        config.setProperty(property.substring(0, index), property.substring(index + 1))
                    }
                }
                bootstrap.config = config
            }

            // Register shutdown hook
            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    // Should trigger some kind of run interruption
                }
            })

            // Do bootstrap
            bootstrap.bootstrap()
            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    bootstrap.shutdown()
                }
            })

            //
            shell = if (interactive) {
                val factory = bootstrap.context.getPlugin(
                    ShellFactory::class.java)
                factory.create(null)
            } else {
                null
            }
            closeable = null
        }

        //
        if (shell != null) {

            //
            val term = TerminalFactory.create()

            //
            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    try {
                        term.restore()
                    } catch (ignore: Exception) {
                    }
                }
            })

            //
            val encoding = Configuration.getEncoding()

            // Use AnsiConsole only if term doesn't support Ansi
            val out: PrintStream
            val err: PrintStream
            val ansi: Boolean
            if (term.isAnsiSupported) {
                out =
                    PrintStream(BufferedOutputStream(term.wrapOutIfNeeded(FileOutputStream(FileDescriptor.out)), 16384),
                        false,
                        encoding)
                err =
                    PrintStream(BufferedOutputStream(term.wrapOutIfNeeded(FileOutputStream(FileDescriptor.err)), 16384),
                        false,
                        encoding)
                ansi = true
            } else {
                out = AnsiConsole.out
                err = AnsiConsole.err
                ansi = false
            }

            //
            val `in` = FileInputStream(FileDescriptor.`in`)
            val reader = ConsoleReader(null, `in`, out, term)

            //
            val processor = JLineProcessor(ansi, shell, reader, out)

            //
            val interruptHandler = InterruptHandler { processor.interrupt() }
            interruptHandler.install()

            //
            val thread = Thread(processor)
            thread.isDaemon = true
            thread.start()

            //
            try {
                processor.closed()
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {

                //
                if (closeable != null) {
                    Utils.close(closeable)
                }

                // Force exit
                System.exit(0)
            }
        }
    }

    companion object {
        /** .  */
        private val log = Logger.getLogger(Shell::class.java.name)
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val line = StringBuilder()
            for (i in args.indices) {
                if (i > 0) {
                    line.append(' ')
                }
                Delimiter.EMPTY.escape(args[i], line)
            }

            //
            val main = Shelll()
            val matcher = main.descriptor.matcher()
            val match = matcher.parse(line.toString())
            match.invoke(Util.wrap(main))
        }
    }
}
