package darkkeks.pixel2019.graphics;

import darkkeks.pixel2019.Controller;
import darkkeks.pixel2020.Template;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;


public class BoardGraphics {
    
    private static final String TITLE = "PxlsCLI";

    private static final int WIDTH = 700;
    private static final int HEIGHT = 400;
    private static final int MOVE_STEP = 250;
    private static final double ZOOM_STEP = Math.sqrt(2);

    private BoardCanvas canvas;
    private JFrame frame;
    private BoardClickListener boardClickListener;

    private AffineTransform transform;
    private double offsetX, offsetY, mousePressedX, mousePressedY;
    private double zoom;
    private boolean isShiftHeld;
    private boolean isCtrlHeld;
    private boolean isAltHeld;
    private boolean dragWithAlt;

    public BoardGraphics(BufferedImage initial) {
        canvas = new BoardCanvas(WIDTH, HEIGHT, initial);
        transform = canvas.getTransform();

        this.offsetX = this.offsetY = 0;
        this.isShiftHeld = this.isCtrlHeld = false;
        this.zoom = 1;

        if(System.getenv("nogui") == null) {
            frame = new JFrame(TITLE);
            frame.add(canvas);
            frame.setResizable(false);
            frame.pack();
            frame.setVisible(true);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            setupKeyListener();
            setupMouseWheelListener();
            setupMouseListener();
        }
    }

    public void updateBoard(BufferedImage board) {
        canvas.updateBoard(board);
    }

    public BufferedImage getImage() {
        return canvas.getImage();
    }

    public void redraw() {
        canvas.repaint();
    }

    public void setPixel(int x, int y, Color color) {
        canvas.setPixel(x, y, color);
    }

    public void setTemplate(Template template) {
        canvas.setTemplate(template);
    }

    public void setBoardClickListener(BoardClickListener listener) {
        this.boardClickListener = listener;
    }

    private double getMoveStep() {
        if(isShiftHeld) return 1;
        if(isCtrlHeld) return 10;
        return MOVE_STEP / zoom;
    }

    private double getWidthInPixels() {
        return WIDTH / zoom;
    }

    private double getHeightInPixels() {
        return HEIGHT / zoom;
    }

    private void zoomInCenter() {
        zoomIn(WIDTH / 2, HEIGHT / 2);
    }

    private void zoomOutCenter() {
        zoomOut(WIDTH / 2, HEIGHT / 2);
    }

    private void zoomIn(int zoomX, int zoomY) {
        if(zoom < 128) {
            zoom *= ZOOM_STEP;
            offsetX += (ZOOM_STEP - 1) * zoomX / zoom;
            offsetY += (ZOOM_STEP - 1) * zoomY / zoom;
        }
    }

    private void zoomOut(int zoomX, int zoomY) {
        if(zoom - 1 > 1e-9) { // floating-point comparison epsilon
            offsetX -= (ZOOM_STEP - 1) * zoomX / zoom;
            offsetY -= (ZOOM_STEP - 1) * zoomY / zoom;
            zoom /= ZOOM_STEP;
        }
    }

    private void checkBorders() {
        offsetX = Math.max(offsetX, -getWidthInPixels() / 2);
        offsetX = Math.min(offsetX, Constants.WIDTH - getWidthInPixels() / 2);
        offsetY = Math.max(offsetY, -getHeightInPixels() / 2);
        offsetY = Math.min(offsetY, Constants.HEIGHT - getHeightInPixels() / 2);
    }

    private void updateTransform() {
        transform.setToScale(zoom, zoom);
        transform.translate(-offsetX, -offsetY);
    }
    
    private void moveTemplate(int dx, int dy) {
        canvas.translateTemplate(dx, dy);
        Controller.requestQueueRebuild();
    }

    private void setupKeyListener() {
        frame.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();

                if (key == KeyEvent.VK_SHIFT) isShiftHeld = true;
                if (key == KeyEvent.VK_CONTROL) isCtrlHeld = true;
                if (key == KeyEvent.VK_ALT) isAltHeld = true;
                
                if (isAltHeld) {
                    int moveStep = (int) Math.ceil(getMoveStep());
    
                    // Move template
                    if (key == KeyEvent.VK_UP) moveTemplate(0, -moveStep);
                    if (key == KeyEvent.VK_LEFT) moveTemplate(-moveStep, 0);
                    if (key == KeyEvent.VK_RIGHT) moveTemplate(moveStep, 0);
                    if (key == KeyEvent.VK_DOWN) moveTemplate(0, moveStep);
                    
                    // Adjust template opacity
                    if (key == KeyEvent.VK_PLUS) canvas.adjustTemplateOpacity(0.1f);
                    if (key == KeyEvent.VK_EQUALS) canvas.adjustTemplateOpacity(0.1f);
                    if (key == KeyEvent.VK_MINUS) canvas.adjustTemplateOpacity(-0.1f);
                } else {
                    // Move canvas
                    if (key == KeyEvent.VK_UP) offsetY -= getMoveStep();
                    if (key == KeyEvent.VK_LEFT) offsetX -= getMoveStep();
                    if (key == KeyEvent.VK_RIGHT) offsetX += getMoveStep();
                    if (key == KeyEvent.VK_DOWN) offsetY += getMoveStep();
                    
                    // Zoom in/out
                    if (key == KeyEvent.VK_PLUS) zoomInCenter();
                    if (key == KeyEvent.VK_EQUALS) zoomInCenter();
                    if (key == KeyEvent.VK_MINUS) zoomOutCenter();
                }
    
                // Toggle template visibility
                if (key == KeyEvent.VK_SPACE) canvas.toggleTemplateVisibility();
    
                // Pause bot
                //if (key == KeyEvent.VK_P) ;
                
                // Show about/help box
                //if (key == KeyEvent.VK_F1) ;

                checkBorders();
                updateTransform();
                redraw();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int key = e.getKeyCode();

                if (key == KeyEvent.VK_SHIFT) isShiftHeld = false;
                if (key == KeyEvent.VK_CONTROL) isCtrlHeld = false;
                if (key == KeyEvent.VK_ALT) isAltHeld = false;
            }
        });
    }

    private void setupMouseWheelListener() {
        canvas.addMouseWheelListener((e) -> {
            int x = e.getX(), y = e.getY();
            if (x < 0 || x > WIDTH ||
                    y < 0 || y > HEIGHT)
                return;
            if(e.getWheelRotation() < 0)
                zoomIn(x, y);
            else if (e.getWheelRotation() > 0)
                zoomOut(x, y);

            checkBorders();
            updateTransform();
            redraw();
        });
    }

    private void setupMouseListener() {
        canvas.addMouseListener(new MouseListener() {
            @Override public void mouseClicked(MouseEvent e) {
                try {
                    Point2D boardPoint = transform.inverseTransform(new Point2D.Double(e.getX(), e.getY()), null);

                    if(boardClickListener != null) {
                        int x = (int)boardPoint.getX();
                        int y = (int)boardPoint.getY();
                        if(Constants.checkRange(x, y)) {
                            boardClickListener.onClick(x, y);
                        }
                    }
                } catch (NoninvertibleTransformException ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                mousePressedX = e.getX();
                mousePressedY = e.getY();
                dragWithAlt = isAltHeld;
            }

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override public void mouseEntered(MouseEvent e) {}

            @Override public void mouseExited(MouseEvent e) {}
        });

        canvas.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragWithAlt) {
                    int ox = (int) ((mousePressedX - e.getX()) / zoom);
                    int oy = (int) ((mousePressedY - e.getY()) / zoom);
    
                    moveTemplate(-ox, -oy);
                    
                    mousePressedX -= ox * zoom;
                    mousePressedY -= oy * zoom;
                } else {
                    offsetX += (mousePressedX - e.getX()) / zoom;
                    offsetY += (mousePressedY - e.getY()) / zoom;
    
                    mousePressedX = e.getX();
                    mousePressedY = e.getY();
                }

                checkBorders();
                updateTransform();
                redraw();
            }

            @Override public void mouseMoved(MouseEvent e) {
                Point point = getPixelUnderCursor(e.getX(), e.getY());
                frame.setTitle(String.format("%s (%d:%d)", TITLE, point.x, point.y));
            }
        });
    }
    
    private Point getPixelUnderCursor(int mouseX, int mouseY) {
        return new Point((int) (mouseX / zoom + offsetX), (int) (mouseY / zoom + offsetY));
    }
}
