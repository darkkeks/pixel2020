package darkkeks.pixel2020;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;


public class Template {

    public static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    private Point minimalPixelCoords;
    private Point maximalPixelCoords;

    private BufferedImage image;

    public Template(BufferedImage image) {
        this.image = image;
        normalizeColors();
        cropImage();
    }

    public int getWidth() {
        return image.getWidth();
    }

    public int getHeight() {
        return image.getHeight();
    }

    public Point getOffset() {
        return minimalPixelCoords;
    }

    public BufferedImage getImage() {
        return image;
    }

    public Color getColor(int x, int y) {
        if (x < 0 || y < 0 || x > getWidth() - 1 || y > getHeight() - 1) return null;

        int rgb = image.getRGB(x, y);
        return Colors.INSTANCE.getRGB_TO_COLOR().get(rgb);
    }

    public Color getColorAbs(int x, int y) {
        return getColor(x - minimalPixelCoords.x, y - minimalPixelCoords.y);
    }

    private int colorDist(Color a, Color b) {
        return (a.getRed() - b.getRed()) * (a.getRed() - b.getRed()) +
                (a.getGreen() - b.getGreen()) * (a.getGreen() - b.getGreen()) +
                (a.getBlue() - b.getBlue()) * (a.getBlue() - b.getBlue());
    }

    private void normalizeColors() {
        for (int x = 0; x < image.getWidth(); ++x) {
            for (int y = 0; y < image.getHeight(); ++y) {
                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb, image.getAlphaRaster() != null);
                if(color.getAlpha() < 40) {
                    image.setRGB(x, y, TRANSPARENT.getRGB());
                } else {
                    Color best = null;
                    double bestDist = Integer.MAX_VALUE;
                    for (Color palette : Colors.INSTANCE.getRGB_MAP()) {
                        int dist = colorDist(palette, color);
                        if (dist < bestDist) {
                            best = palette;
                            bestDist = dist;
                        }
                    }
                    assert best != null;
                    image.setRGB(x, y, best.getRGB());
                }
            }
        }
    }

    private void cropImage() {
        Point min = new Point(getWidth(),getHeight());
        Point max = new Point(0, 0);

        BufferedImage croppedImage;

        for (int x = 0; x < image.getWidth(); ++x) {
            for (int y = 0 ; y < image.getHeight() ; ++y) {
                int rgb = image.getRGB(x, y);
                Color color = new Color(rgb, image.getAlphaRaster() != null);
                if (!color.equals(TRANSPARENT)) {
                    // Adjust min and max pixels
                    min.setLocation(Math.min(min.x, x), Math.min(min.y, y));
                    max.setLocation(Math.max(max.x, x), Math.max(max.y, y));
                }
            }
        }

        int w = max.x - min.x + 1;
        int h = max.y - min.y + 1;

        croppedImage = image.getSubimage(min.x, min.y, w, h);

        image = croppedImage;
        minimalPixelCoords = min;
        maximalPixelCoords = max;
    }

    public Set<TemplatePixel> getPixels() {
        Set<TemplatePixel> pixels = new HashSet<>();

        for (int x = 0; x < getWidth(); x++) {
            for (int y = 0; y < getHeight(); y++) {
                int absX = minimalPixelCoords.x + x;
                int absY = minimalPixelCoords.y + y;
                TemplatePixel pixel = new TemplatePixel(x, y, absX, absY, getColor(x, y));

                pixels.add(pixel);
            }
        }

        return pixels;
    }

    public void setLocation(int x, int y) {
        minimalPixelCoords.setLocation(x, y);
        maximalPixelCoords.setLocation(x + getWidth(), y + getHeight());
    }

    public static class TemplatePixel {

        public final int x;
        public final int y;
        public final int absX;
        public final int absY;
        public final Color color;

        public TemplatePixel(int x, int y, int absX, int absY, Color color) {
            this.x = x;
            this.y = y;
            this.absX = absX;
            this.absY = absY;
            this.color = color;
        }

    }
}
