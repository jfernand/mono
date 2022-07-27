import jexer.TAction
import jexer.TApplication
import jexer.TDesktop
import jexer.TDirectoryList
import jexer.TImage
import jexer.TKeypress
import jexer.backend.SwingTerminal
import jexer.bits.GraphicsChars
import jexer.event.TKeypressEvent
import jexer.event.TResizeEvent
import jexer.menu.TMenu
import jexer.ttree.TDirectoryTreeItem
import jexer.ttree.TTreeViewWidget
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

/**
 * Implements a simple image thumbnail file viewer.  Much of this code was
 * stripped down from TFileOpenBox.
 */
class JexerImageViewer : TApplication(BackendType.XTERM) {
    /**
     * Public constructor chooses the ECMA-48 / Xterm backend.
     */
    init {

        // The stock tool menu has items for redrawing the screen, opening
        // images, and (when using the Swing backend) setting the font.
        addToolMenu()

        // We will have one menu containing a mix of new and stock commands
        val fileMenu = addMenu("&File")

        // Stock commands: a new shell, exit program.
        fileMenu.addDefaultItem(TMenu.MID_SHELL)
        fileMenu.addSeparator()
        fileMenu.addDefaultItem(TMenu.MID_EXIT)

        // Filter the files list to support image suffixes only.
        val filters: MutableList<String> = ArrayList()
        filters.add("^.*\\.[Jj][Pp][Gg]$")
        filters.add("^.*\\.[Jj][Pp][Ee][Gg]$")
        filters.add("^.*\\.[Pp][Nn][Gg]$")
        filters.add("^.*\\.[Gg][Ii][Ff]$")
        filters.add("^.*\\.[Bb][Mm][Pp]$")
        desktop = ImageViewerDesktop(this, ".", filters)
    }

    companion object {
        /**
         * Main entry point.
         */
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val app = JexerImageViewer()
            Thread(app).start()
        }
    }
}

/**
 * The desktop contains a tree view on the left, list of files on the top
 * right, and image view on the bottom right.
 */
internal class ImageViewerDesktop(
    application: TApplication?, path: String?,
    filters: List<String>?,
) : TDesktop(application) {
    /**
     * The left-side tree view pane.
     */
    val treeView: TTreeViewWidget

    /**
     * The data behind treeView.
     */
    private val treeViewRoot: TDirectoryTreeItem

    /**
     * The top-right-side directory list pane.
     */
    public val directoryList: TDirectoryList

    /**
     * The bottom-right-side image pane.
     */
    private var imageWidget: TImage? = null

    /**
     * Public constructor.
     *
     * @param application the TApplication that manages this window
     * @param path path of selected file
     * @param filters a list of strings that files must match to be displayed
     * @throws IOException of a java.io operation throws
     */
    init {
        isActive = true

        // Add directory treeView
        val value = object : TAction() {
            override fun DO() {
                val item = treeView.selected
                val selectedDir = (item as TDirectoryTreeItem).file
                try {
                    directoryList.setPath(selectedDir.canonicalPath)
                    if (directoryList.list.size > 0) {
                        setThumbnail(directoryList.path)
                    } else {
                        if (imageWidget != null) {
                            children.remove(imageWidget)
                        }
                        imageWidget = null
                    }
                    activate(treeView)
                } catch (e: IOException) {
                    // If the backend is Swing, we can emit the stack
                    // trace to stderr.  Otherwise, just squash it.
                    if (screen is SwingTerminal) {
                        e.printStackTrace()
                    }
                }
            }
        }
        treeView = addTreeViewWidget(0, 0, width / 2, height,
            value
        )
        treeViewRoot = TDirectoryTreeItem(treeView, path, true)

        // Add directory files list
        directoryList = addDirectoryList(path, width / 2 + 1, 0,
            width / 2 - 1, height / 2,
            object : TAction() {
                override fun DO() {
                    setThumbnail(directoryList.path)
                }
            },
            object : TAction() {
                override fun DO() {
                    setThumbnail(directoryList.path)
                }
            },
            filters)
        if (directoryList.list.size > 0) {
            activate(directoryList)
            setThumbnail(directoryList.path)
        } else {
            activate(treeView)
        }
    }

    /**
     * Handle window/screen resize events.
     *
     * @param event resize event
     */
    override fun onResize(event: TResizeEvent) {

        // Resize the tree and list
        treeView.y = 1
        treeView.width = width / 2
        treeView.height = height - 1
        treeView.onResize(TResizeEvent(event.backend,
            TResizeEvent.Type.WIDGET,
            treeView.width,
            treeView.height))
        treeView.treeView.onResize(TResizeEvent(event.backend,
            TResizeEvent.Type.WIDGET,
            treeView.width - 1,
            treeView.height - 1))
        directoryList.x = width / 2 + 1
        directoryList.y = 1
        directoryList.width = width / 2 - 1
        directoryList.height = height / 2 - 1
        directoryList.onResize(TResizeEvent(event.backend,
            TResizeEvent.Type.WIDGET,
            directoryList.width,
            directoryList.height))

        // Recreate the image
        if (imageWidget != null) {
            children.remove(imageWidget)
        }
        imageWidget = null
        if (directoryList.list.size > 0) {
            activate(directoryList)
            setThumbnail(directoryList.path)
        } else {
            activate(treeView)
        }
    }

    /**
     * Handle keystrokes.
     *
     * @param keypress keystroke event
     */
    override fun onKeypress(keypress: TKeypressEvent) {
        if (treeView.isActive || directoryList.isActive) {
            if (keypress == TKeypress.kbEnter
                || keypress == TKeypress.kbUp
                || keypress == TKeypress.kbDown
                || keypress == TKeypress.kbPgUp
                || keypress == TKeypress.kbPgDn
                || keypress == TKeypress.kbHome
                || keypress == TKeypress.kbEnd
            ) {
                // Tree view will be changing, update the directory list.
                super.onKeypress(keypress)

                // This is the same action as treeView's enter.
                val item = treeView.selected
                val selectedDir = (item as TDirectoryTreeItem).file
                try {
                    if (treeView.isActive) {
                        directoryList.setPath(selectedDir.canonicalPath)
                    }
                    if (directoryList.list.size > 0) {
                        activate(directoryList)
                        setThumbnail(directoryList.path)
                    } else {
                        if (imageWidget != null) {
                            children.remove(imageWidget)
                        }
                        imageWidget = null
                        activate(treeView)
                    }
                } catch (e: IOException) {
                    // If the backend is Swing, we can emit the stack trace
                    // to stderr.  Otherwise, just squash it.
                    if (screen is SwingTerminal) {
                        e.printStackTrace()
                    }
                }
                return
            }
        }

        // Pass to my parent
        super.onKeypress(keypress)
    }

    /**
     * Draw me on screen.
     */
    override fun draw() {
        val background = theme.getColor("tdesktop.background")
        putAll(' '.code, background)
        vLineXY(width / 2, 0, height,
            GraphicsChars.WINDOW_SIDE.code, getBackground())
        hLineXY(width / 2, height / 2, (width + 1) / 2,
            GraphicsChars.WINDOW_TOP.code, getBackground())
        putCharXY(width / 2, height / 2,
            GraphicsChars.WINDOW_LEFT_TEE.code, getBackground())
    }

    /**
     * Set the image thumbnail.
     *
     * @param file the image file
     */
    private fun setThumbnail(file: File?) {
        if (file == null) {
            return
        }
        if (!file.exists() || !file.isFile) {
            return
        }
        var image: BufferedImage? = null
        image = try {
            ImageIO.read(file)
        } catch (e: IOException) {
            // If the backend is Swing, we can emit the stack trace to
            // stderr.  Otherwise, just squash it.
            if (screen is SwingTerminal) {
                e.printStackTrace()
            }
            return
        }
        if (imageWidget != null) {
            children.remove(imageWidget)
        }
        val width = width / 2 - 1
        val height = height / 2 - 1
        imageWidget = TImage(this, getWidth() - width,
            getHeight() - height, width, height, image, 0, 0, null)

        // Resize the image to fit within the pane.
        imageWidget!!.scaleType = TImage.Scale.SCALE
        imageWidget!!.isActive = false
        activate(directoryList)
    }
}

class TreeviewAction(
    val treeView: TTreeViewWidget,
    val directoryList:TDirectoryList,
    ) : TAction() {
    override fun DO() {
        val item = treeView.selected
        val selectedDir = (item as TDirectoryTreeItem).file
        try {
            directoryList.setPath(selectedDir.canonicalPath)
            if (directoryList.list.size > 0) {
                setThumbnail(directoryList.path)
            } else {
                if (imageWidget != null) {
                    children.remove(imageWidget)
                }
                imageWidget = null
            }
            activate(treeView)
        } catch (e: IOException) {
            // If the backend is Swing, we can emit the stack
            // trace to stderr.  Otherwise, just squash it.
            if (screen is SwingTerminal) {
                e.printStackTrace()
            }
        }
    }
}
