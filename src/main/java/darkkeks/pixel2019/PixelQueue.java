package darkkeks.pixel2019;

import darkkeks.pixel2020.Pixel;
import darkkeks.pixel2020.Template;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static darkkeks.pixel2020.PixelApiKt.FIELD_HEIGHT;
import static darkkeks.pixel2020.PixelApiKt.FIELD_WIDTH;

public class PixelQueue {

    private Template template;
    private TreeSet<Point> queue;
    private Map<Point, Integer> order;

    public PixelQueue(Template template) {
        this.template = template;
        this.queue = new TreeSet<>(Comparator.comparingInt(p -> order.get(p)));

        this.order = new ConcurrentHashMap<>();
        for(int i = 0; i < FIELD_WIDTH; ++i) {
            for(int j = 0; j < FIELD_HEIGHT; ++j) {
                order.put(new Point(i, j), ThreadLocalRandom.current().nextInt());
            }
        }
    }

    public synchronized void rebuild(BufferedImage currentBoard) {
        queue.clear();
        java.awt.Point offset = template.getOffset();
        
        for (Template.TemplatePixel pixel : template.getPixels())
            if (pixel.color != null
                    && currentBoard.getRGB(offset.x + pixel.x, offset.y + pixel.y) != pixel.color.getRGB())
                queue.add(new Point(offset.x + pixel.x, offset.y + pixel.y));
        
        //for(int i = 0; i < Constants.WIDTH; ++i) {
        //    for(int j = 0; j < Constants.HEIGHT; ++j) {
        //        Color target = template.getColor(i, j);
        //        if(target != null && currentBoard.getRGB(i, j) != target.getRGB()) {
        //            queue.add(new Point(i, j));
        //        }
        //    }
        //}
    }

    public synchronized void onPixelChange(Pixel pixel) {
        Color target = template.getColorAbs(pixel.getX(), pixel.getY());
        if(target == null) return;

        Color color = pixel.getColor();
        if(color == target) {
            queue.remove(new Point(pixel.getX(), pixel.getY()));
        } else {
            queue.add(new Point(pixel.getX(), pixel.getY()));
        }
    }

    public synchronized Point pop() {
        Point result = queue.first();
        queue.remove(result);
        return result;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

    public static class Point {
        private int x;
        private int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Point point = (Point) o;
            return x == point.x &&
                    y == point.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

}
