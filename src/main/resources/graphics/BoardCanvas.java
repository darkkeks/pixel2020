package darkkeks.pixel2019.graphics;

import darkkeks.pixel2020.Template;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;

public class BoardCanvas extends JPanel {

    private final int width;
    private final int height;
    private BufferedImage canvas;
    private final AffineTransform transform;

    private Template template;

    private float templateOpacity;
    private boolean templateVisible = true;

    public BoardCanvas(int width, int height, BufferedImage canvas) {
        this.width = width;
        this.height = height;
        this.canvas = canvas;
        this.transform = new AffineTransform();

        this.templateOpacity = 0.7f;

        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.drawImage(canvas, transform, null);

        if(template != null && templateVisible) {
            RescaleOp filter = new RescaleOp(new float[]{1f, 1f, 1f, templateOpacity}, new float[4], null);
            BufferedImage filtered = filter.filter(template.getImage(), null);
            
            AffineTransform templateTransform = new AffineTransform(transform);
            templateTransform.translate(template.getOffset().x, template.getOffset().y);
            
            g2.drawImage(filtered, templateTransform, null);
        }
    }

    public void updateBoard(BufferedImage board) {
        this.canvas = board;
        repaint();
    }

    public void setTemplate(Template template) {
        this.template = template;
        repaint();
    }
    
    public void adjustTemplateOpacity(float diff) {
        setTemplateOpacity(templateOpacity + diff);
    }
    
    public void setTemplateOpacity(float opacity) {
        opacity = Math.max(Math.min(opacity, 1f), 0f);
        templateOpacity = opacity;
    }
    
    public void toggleTemplateVisibility() {
        setTemplateVisibility(!templateVisible);
    }
    
    public void setTemplateVisibility(boolean visibility) {
        templateVisible = visibility;
    }
    
    public boolean isTemplateVisible() {
        return templateVisible;
    }
    
    public void translateTemplate(int dx, int dy) {
        setTemplateLocation(template.getOffset().x + dx, template.getOffset().y + dy);
    }
    
    public void setTemplateLocation(int x, int y) {
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (x > canvas.getWidth() - template.getWidth()) x = canvas.getWidth() - template.getWidth();
        if (y > canvas.getHeight() - template.getHeight()) y = canvas.getHeight() - template.getHeight();
        
        template.setLocation(x, y);
    }
    
    public AffineTransform getTransform() {
        return transform;
    }

    public void setPixel(int x, int y, Color color) {
        canvas.setRGB(x, y, color.getRGB());
        repaint();
    }

    public BufferedImage getImage() {
        return canvas;
    }
}
