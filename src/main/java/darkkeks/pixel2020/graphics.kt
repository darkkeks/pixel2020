package darkkeks.pixel2020

import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.event.MouseWheelEvent
import java.awt.geom.AffineTransform
import java.awt.geom.NoninvertibleTransformException
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.awt.image.RescaleOp
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.WindowConstants
import kotlin.math.sqrt

class Canvas(
    val windowWidth: Int,
    val windowHeight: Int,
    initialCanvas: BufferedImage
) : JPanel() {

    val transform = AffineTransform()

    var canvas: BufferedImage = initialCanvas
    var template: Template? = null

    var templateOpacity = 0.7
    var isTemplateVisible = true

    init {
        repaint()
    }

    override fun getPreferredSize() = Dimension(windowWidth, windowHeight)

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        if (g !is Graphics2D) return

        g.drawImage(canvas, transform, null)

        if (isTemplateVisible) {
            template?.also { template ->
                val filter = RescaleOp(floatArrayOf(1f, 1f, 1f, templateOpacity.toFloat()), FloatArray(4), null)
                val filtered = filter.filter(template.image, null)
                val templateTransform = AffineTransform(transform)
                templateTransform.translate(template.offset.x.toDouble(), template.offset.y.toDouble())
                g.drawImage(filtered, templateTransform, null)
            }
        }
    }

    fun setPixel(x: Int, y: Int, color: Color) {
        canvas.setRGB(x, y, color.rgb);
    }
}

class BoardGraphics(initialCanvas: BufferedImage) {

    private val title = "Pixel2020"
    private val zoomStep = sqrt(2.0)

    private val canvas = Canvas(700, 400, initialCanvas)

    private var offsetX = 0.0
    private var offsetY = 0.0
    private var mousePressedX = 0.0
    private var mousePressedY = 0.0
    private var zoom = 1.0
    private var isShiftHeld = false
    private var isCtrlHeld = false
    private var isAltHeld = false
    private var dragWithAlt = false

    private var frame: JFrame

    val image get() = canvas.canvas

    init {
        frame = JFrame(title).apply {
            add(canvas)
            setResizable(false)
            pack()
            setVisible(true)
            setLocationRelativeTo(null)
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
        }

//        setupKeyListener()
        setupMouseWheelListener()
        setupMouseListener()
    }

    private fun setupMouseWheelListener() {
        canvas.addMouseWheelListener { e: MouseWheelEvent ->
            if (e.x >= 0 && e.x < canvas.windowWidth && e.y >= 0 && e.y < canvas.height) {
                if (e.wheelRotation < 0) {
                    zoomIn(e.x, e.y)
                } else if (e.wheelRotation > 0) {
                    zoomOut(e.x, e.y)
                }
                checkBorders()
                updateTransform()
                canvas.repaint()
            }
        }
    }

    private fun setupMouseListener() {
        canvas.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent) {
                val boardPoint: Point2D = try {
                    canvas.transform.inverseTransform(
                        Point2D.Double(
                            e.x.toDouble(),
                            e.y.toDouble()
                        ), null
                    )
                } catch (ex: NoninvertibleTransformException) {
                    throw IllegalStateException("Wtf", ex)
                }

//                if (boardClickListener != null) {
//                    val x = boardPoint.x.toInt()
//                    val y = boardPoint.y.toInt()
//                    if (Constants.checkRange(x, y)) {
//                        boardClickListener.onClick(x, y)
//                    }
//                }
            }

            override fun mousePressed(e: MouseEvent) {
                mousePressedX = e.x.toDouble()
                mousePressedY = e.y.toDouble()
                dragWithAlt = isAltHeld
            }

            override fun mouseReleased(e: MouseEvent) {}
            override fun mouseEntered(e: MouseEvent) {}
            override fun mouseExited(e: MouseEvent) {}
        })
        canvas.addMouseMotionListener(object : MouseMotionListener {
            override fun mouseDragged(e: MouseEvent) {
                if (dragWithAlt) {
//                    val ox = ((mousePressedX - e.x) / zoom).toInt()
//                    val oy = ((mousePressedY - e.y) / zoom).toInt()
//                    moveTemplate(-ox, -oy)
//                    mousePressedX -= ox * zoom
//                    mousePressedY -= oy * zoom
                } else {
                    offsetX += (mousePressedX - e.x) / zoom
                    offsetY += (mousePressedY - e.y) / zoom
                    mousePressedX = e.x.toDouble()
                    mousePressedY = e.y.toDouble()
                }
                checkBorders()
                updateTransform()
                canvas.repaint()
            }

            override fun mouseMoved(e: MouseEvent) {
                val point = Point((e.x / zoom + offsetX).toInt(), (e.y / zoom + offsetY).toInt())
                frame.title = String.format("%s (%d:%d)", title, point.x, point.y)
            }
        })
    }

    private fun zoomIn(zoomX: Int, zoomY: Int) {
        if (zoom < 128) {
            zoom *= zoomStep
            offsetX += (zoomStep - 1) * zoomX / zoom
            offsetY += (zoomStep - 1) * zoomY / zoom
        }
    }

    private fun zoomOut(zoomX: Int, zoomY: Int) {
        if (zoom - 1 > 1e-9) {
            offsetX -= (zoomStep - 1) * zoomX / zoom
            offsetY -= (zoomStep - 1) * zoomY / zoom
            zoom /= zoomStep
        }
    }

    private fun checkBorders() {
        val pixelWidth = canvas.windowWidth / zoomStep;
        val pixelHeight = canvas.windowHeight / zoomStep;
        offsetX = offsetX.coerceIn(-pixelWidth / 2, FIELD_WIDTH - pixelWidth / 2)
        offsetY = offsetY.coerceIn(-pixelHeight / 2, FIELD_HEIGHT - pixelHeight / 2)
    }

    private fun updateTransform() {
        canvas.transform.setToScale(zoom, zoom)
        canvas.transform.translate(-offsetX, -offsetY)
    }

    fun updateBoard(image: BufferedImage) {
        canvas.canvas = image
        canvas.repaint()
    }

    fun updateTemplate(template: Template) {
        canvas.template = template
        canvas.repaint()
    }

    fun setPixel(x: Int, y: Int, color: Color) {
        canvas.setPixel(x, y, color)
        canvas.repaint()
    }
}
