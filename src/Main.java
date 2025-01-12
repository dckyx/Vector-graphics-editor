import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

// ---------------------- Main Application ----------------------
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Vector Graphics Editor");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 1400);

            DrawingPanel drawingPanel = new DrawingPanel();
            ToolBar toolbar = new ToolBar(drawingPanel);
            drawingPanel.setToolBar(toolbar);

            toolbar.addShapeObserver(drawingPanel);
            frame.add(drawingPanel, BorderLayout.CENTER);
            frame.add(toolbar, BorderLayout.NORTH);

            frame.setVisible(true);
        });
    }
}

// ---------------------- DrawingPanel (View + partial Controller) ----------------------
class DrawingPanel extends JPanel implements ShapeObserver{
    private final List<ColoredShape> shapes = new ArrayList<>();
    private ColoredShape currentShape = null;

    private double startX, startY;
    private double offsetX, offsetY;
    private ColoredShape selectedShape = null;
    private Color currentColor = Color.BLACK;


    private ToolBar toolBar;

    // Command Pattern
    private final CommandManager commandManager = new CommandManager();

    public DrawingPanel() {
        setBackground(Color.WHITE);
        setCursor(Cursor.getDefaultCursor());

        addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                startX = e.getX();
                startY = e.getY();

                String tool = toolBar.getCurrentTool();

                if ("Move".equals(tool)) {
                    selectedShape = findShapeAt(e.getX(), e.getY());
                    if (selectedShape != null) {
                        offsetX = e.getX() - selectedShape.getX();
                        offsetY = e.getY() - selectedShape.getY();
                    }
                    return;
                }


                currentShape = ShapeFactory.createShape(tool, startX, startY);

                if (currentShape != null) {
                    currentShape.setColor(toolBar.getSelectedColor());
                    currentShape.setLineSize(toolBar.getLineSize());

//                    if ("Polygon".equals(tool)) {
//                        ((PolygonShape) currentShape).addPoint((int) startX, (int) startY);
//                    }
//                    else
                        if ("Brush".equals(tool)) {
                        ((BrushShape) currentShape).addPoint(startX, startY);
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                String tool = toolBar.getCurrentTool();
                if ("Move".equals(tool) && selectedShape != null) {
                        double oldX = selectedShape.getX();
                        double oldY = selectedShape.getY();
                        commandManager.executeCommand(new MoveCommand(
                                selectedShape, oldX, oldY, selectedShape.getX(), selectedShape.getY()
                        ));
                        selectedShape = null;
                        repaint();
                        return;
                    }
                    if (currentShape != null) {
                        if ("Brush".equals(tool)) {
                            commandManager.executeCommand(new AddShapeCommand(DrawingPanel.this, currentShape));
                        } else if (!"Polygon".equals(tool)) {
                            commandManager.executeCommand(new AddShapeCommand(DrawingPanel.this, currentShape));
                        }

                        currentShape = null;
                        repaint();
                    }
                if ("Polygon".equals(tool)) {
                    if (currentShape == null) {
                        currentShape = new PolygonShape();
                        currentShape.setColor(currentColor);
                        currentShape.setLineSize(toolBar.getLineSize());
                        currentShape.addPoint(e.getX(), e.getY());
                    }
                }
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                String tool = toolBar.getCurrentTool();

                if ("Polygon".equals(tool)) {
                    if (currentShape == null) {
                        currentShape = new PolygonShape();
                        currentShape.setColor(currentColor);
                        currentShape.setLineSize(toolBar.getLineSize());
                    }
                    PolygonShape poly = (PolygonShape) currentShape;
                    poly.addPoint(e.getX(), e.getY());
                    repaint();

                    if (e.getClickCount() == 2) {
                        poly.closePolygon();
                        commandManager.executeCommand(new AddShapeCommand(DrawingPanel.this, poly));
                        currentShape = null;
                        repaint();
                    }
                } else {
                    super.mouseClicked(e);
                }
            }

        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                String tool = toolBar.getCurrentTool();
                if ("Move".equals(tool) && selectedShape != null) {
                    double newX = e.getX() - offsetX;
                    double newY = e.getY() - offsetY;
                    selectedShape.move(newX, newY);
                    repaint();
                    return;
                }
                if (currentShape == null) return;

                double currentX = e.getX();
                double currentY = e.getY();

                //dynamiczne rysowanie
                if (currentShape instanceof BrushShape brush) {
                    brush.addPoint(currentX, currentY);
                } else if (currentShape instanceof ArcShape arc) {
                    double dx = e.getX() - startX;
                    double dy = e.getY() - startY;

                    double w = Math.abs(dx);
                    double h = Math.abs(dy);
                    double newX = Math.min(startX, e.getX());
                    double newY = Math.min(startY, e.getY());

                    arc.setFlipped(dy < 0);
                    arc.setBounds(newX, newY, w, h);
                }else if (currentShape instanceof LineShape line) {
                    double dx = currentX - startX;
                    double dy = currentY - startY;
                    line.setBounds(startX, startY, dx, dy);
                }else{
                    double width  = Math.abs(currentX - startX);
                    double height = Math.abs(currentY - startY);
                    double newX   = Math.min(startX, currentX);
                    double newY   = Math.min(startY, currentY);

                    currentShape.setBounds(newX, newY, width, height);
                }

                repaint();
            }
        });

    }
    private ColoredShape findShapeAt(double px, double py) {
        for (int i = shapes.size() - 1; i >= 0; i--) {
            ColoredShape s = shapes.get(i);
            if (s.contains(px, py)) {
                return s;
            }
        }
        return null;
    }

    // Prosta metoda – np. sprawdzamy bounding box
    private boolean isInsideShape(ColoredShape shape, double px, double py) {
        // Można weryfikować bounding box lub sam path
        double x = shape.getX();
        double y = shape.getY();
        double w = shape.getWidth();
        double h = shape.getHeight();
        if (px >= x && px <= x + w && py >= y && py <= y + h) {
            return true;
        }
        return false;
    }

    // Obsługa Undo/Redo
    public void undo() {
        commandManager.undo();
        repaint();
    }

    public void redo() {
        commandManager.redo();
        repaint();
    }

    public void addShape(ColoredShape shape) {
        shapes.add(shape);
        repaint();
    }

    public void removeShape(ColoredShape shape) {
        shapes.remove(shape);
        repaint();
    }

    public void moveShape(ColoredShape shape, double newX, double newY) {
        double oldX = shape.getX();
        double oldY = shape.getY();
        commandManager.executeCommand(new MoveCommand(shape, oldX, oldY, newX, newY));
        repaint();
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        for (ColoredShape shape : shapes) {
            shape.paint(g2d);
        }
        if (currentShape != null) {
            currentShape.paint(g2d);
        }
    }

    @Override
    public void onShapeSelected(String shapeName) {
        if ("Move".equals(shapeName)) {
            setCursor(Cursor.getDefaultCursor());
        } else {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        }
    }

    public void setToolBar(ToolBar toolBar) {
        this.toolBar = toolBar;
    }
}

// ---------------------- ToolBar (Controller) ----------------------
class ToolBar extends JToolBar {
    private String currentTool = "Rectangle";
    private Color selectedColor = Color.BLACK;
    private int lineSize = 1;
    private final List<ShapeObserver> observers = new ArrayList<>();


    public ToolBar(DrawingPanel drawingPanel) {
        JSlider strokeSlider = new JSlider(1, 21, lineSize);
        strokeSlider.addChangeListener(e -> lineSize = strokeSlider.getValue());

        addButton("Rectangle", () -> currentTool = "Rectangle");
        addButton("Ellipse",   () -> currentTool = "Ellipse");
        addButton("Circle",    () -> currentTool = "Circle");
        addButton("Polygon",   () -> currentTool = "Polygon");
        addButton("Arc",       () -> currentTool = "ArcShape");
        addButton("Line",      () -> currentTool = "Line");
        addButton("Brush",     () -> currentTool = "Brush");
        add(strokeSlider);
        addButton("Move",      () -> currentTool = "Move");
        addButton("Undo",      drawingPanel::undo);
        addButton("Redo",      drawingPanel::redo);

        JButton colorButton = new JButton("Color");
        colorButton.addActionListener(e -> {
            Color c = JColorChooser.showDialog(drawingPanel, "Choose a color", selectedColor);
            if (c != null) selectedColor = c;
        });
        add(colorButton);


    }

    private void addButton(String name, Runnable action) {
        JButton btn = new JButton(name);
        btn.addActionListener(e ->{
            setCurrentTool(name);
            action.run();
        });
        add(btn);
    }

    public String getCurrentTool() {
        return currentTool;
    }

    public Color getSelectedColor() {
        return selectedColor;
    }

    public int getLineSize() {
        return lineSize;
    }
    private void setCurrentTool(String tool) {
        currentTool = tool;
        notifyObservers(tool);
    }
    public void addShapeObserver(ShapeObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers(String tool) {
        for (ShapeObserver observer : observers) {
            observer.onShapeSelected(tool);
        }
    }
}

// ---------------------- CommandManager ----------------------
class CommandManager {
    private final Stack<Command> undoStack = new Stack<>();
    private final Stack<Command> redoStack = new Stack<>();

    public void executeCommand(Command cmd) {
        cmd.execute();
        undoStack.push(cmd);
        redoStack.clear();
        System.out.println("Command executed. Undo stack size: " + undoStack.size());
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            Command cmd = undoStack.pop();
            cmd.undo();
            redoStack.push(cmd);
            System.out.println("Undo performed. Undo stack size: " + undoStack.size());
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            Command cmd = redoStack.pop();
            cmd.redo();
            undoStack.push(cmd);
            System.out.println("Redo performed. Redo stack size: " + redoStack.size());
        }
    }
}

// ---------------------- Commands ----------------------
interface Command {
    void execute();
    void undo();
    void redo();
}

class AddShapeCommand implements Command {
    private final DrawingPanel panel;
    private final ColoredShape shape;

    public AddShapeCommand(DrawingPanel panel, ColoredShape shape) {
        this.panel = panel;
        this.shape = shape;
    }

    @Override
    public void execute() {
        panel.addShape(shape);
    }

    @Override
    public void undo() {
        panel.removeShape(shape);
    }

    @Override
    public void redo() {
        execute();
    }
}

class MoveCommand implements Command {
    private final ColoredShape shape;
    private final double oldX, oldY;
    private final double newX, newY;

    public MoveCommand(ColoredShape shape, double oldX, double oldY, double newX, double newY) {
        this.shape = shape;
        this.oldX = oldX;
        this.oldY = oldY;
        this.newX = newX;
        this.newY = newY;
    }

    @Override
    public void execute() {
        shape.move(newX, newY);
    }

    @Override
    public void undo() {
        shape.move(oldX, oldY);
    }

    @Override
    public void redo() {
        execute();
    }
}

// ---------------------- Shape Factory (Factory Method) ----------------------
class ShapeFactory {
    public static ColoredShape createShape(String tool, double x, double y) {
        switch (tool) {
            case "Rectangle" -> {
                return new RectangleShape(x, y, 0, 0);
            }
            case "Ellipse" -> {
                return new EllipseShape(x, y, 0, 0, false);
            }
            case "Circle" -> {
                return new EllipseShape(x, y, 0, 0, true);
            }
            case "Polygon" -> {
                return new PolygonShape();
            }
            case "ArcShape" -> {
                return new ArcShape(x, y, 0, 0, 0, 90);
            }
            case "Brush" -> {
                return new BrushShape(x, y);
            }
            case "Line" -> {
                return new LineShape(x, y, x, y);
            }
            default -> {
                return null;
            }
        }
    }
}

// ---------------------- Abstract Classes & Concrete Shapes ----------------------

abstract class ColoredShape {
    protected Color color = Color.BLACK;
    public abstract Rectangle2D getBoundingBox();

    protected float lineSize = 1f;

    public void setColor(Color c) {
        this.color = c;
    }
    public boolean contains(double px, double py) {
        return getBoundingBox().contains(px, py);
    }

    public void setLineSize(float lineSize) {
        this.lineSize = lineSize;
    }
    public abstract double getX();
    public abstract double getY();
    public abstract void paint(Graphics2D g2d);
    public abstract void setBounds(double x, double y, double w, double h);

    // Przykładowa metoda do przesuwania kształtów
    public void move(double newX, double newY) {
        // Możesz zostawić pustą lub zaimplementować w klasach pochodnych
    }

    public abstract double getWidth();
    public abstract double getHeight();

    public void addPoint(int x, int y) {
    }
}

// RectangleShape
class RectangleShape extends ColoredShape {
    private final Rectangle2D.Double rect = new Rectangle2D.Double();

    public RectangleShape(double x, double y, double w, double h) {
        rect.setFrame(x, y, w, h);
    }

    @Override
    public Rectangle2D getBoundingBox() {
        return rect.getBounds2D();
    }

    @Override
    public double getX() {
        return rect.getX();
    }

    @Override
    public double getY() {
        return rect.getY();
    }

    @Override
    public void paint(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(lineSize));
        g2d.draw(rect);
    }

    @Override
    public void setBounds(double x, double y, double w, double h) {
        rect.setFrame(x, y, w, h);
    }

    @Override
    public double getWidth() {
        return rect.getWidth();
    }

    @Override
    public double getHeight() {
        return rect.getHeight();
    }
    @Override
    public void move(double newX, double newY) {
        rect.setFrame(newX, newY, rect.width, rect.height);
    }
}

class EllipseShape extends ColoredShape {
    private final Ellipse2D.Double ellipse = new Ellipse2D.Double();
    private final boolean circle;

    public EllipseShape(double x, double y, double w, double h, boolean circle) {
        this.circle = circle;
        ellipse.setFrame(x, y, w, h);
    }

    @Override
    public Rectangle2D getBoundingBox() {
        return ellipse.getBounds2D();
    }

    @Override
    public double getX() {
        return ellipse.getX();
    }

    @Override
    public double getY() {
        return ellipse.getY();
    }

    @Override
    public void paint(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(lineSize));
        g2d.draw(ellipse);
    }

    @Override
    public void setBounds(double x, double y, double w, double h) {
        if (circle) {
            double size = Math.min(w, h);
            ellipse.setFrame(x, y, size, size);
        } else {
            ellipse.setFrame(x, y, w, h);
        }
    }
    @Override
    public void move(double newX, double newY) {
        ellipse.setFrame(newX, newY, ellipse.width, ellipse.height);
    }

    @Override
    public double getWidth() {
        return ellipse.getWidth();
    }

    @Override
    public double getHeight() {
        return ellipse.getHeight();
    }
}

// LineShape
class LineShape extends ColoredShape {
    private final Line2D.Double line = new Line2D.Double();

    public LineShape(double x1, double y1, double x2, double y2) {
        line.setLine(x1, y1, x2, y2);
    }

    @Override
    public Rectangle2D getBoundingBox() {
        return line.getBounds2D();
    }

    @Override
    public double getX() {
        return line.getX1();
    }

    @Override
    public double getY() {
        return line.getY1();
    }

    @Override
    public void paint(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(lineSize));
        g2d.draw(line);
    }

    @Override
    public void setBounds(double x, double y, double w, double h) {
        // x,y to lewy górny, x+w, y+h to prawy dolny
        line.setLine(x, y, x + w, y + h);
    }

    @Override
    public void move(double newX, double newY) {
        double dx = newX - line.x1;
        double dy = newY - line.y1;
        line.setLine(line.x1 + dx, line.y1 + dy, line.x2 + dx, line.y2 + dy);
    }

    @Override
    public double getWidth() {
        return 0;
    }

    @Override
    public double getHeight() {
        return 0;
    }
}

// ArcShape (łuk)
class ArcShape extends ColoredShape {
    private final Arc2D.Double arc = new Arc2D.Double();

    public ArcShape(double x, double y, double w, double h, double start, double extent) {
        arc.setFrame(x, y, w, h);
        arc.setAngleStart(0);
        arc.setAngleExtent(180);
        arc.setArcType(Arc2D.OPEN);
    }

    @Override
    public Rectangle2D getBoundingBox() {
        return arc.getBounds2D();
    }

    @Override
    public double getX() {
        return arc.getX();
    }

    @Override
    public double getY() {
        return arc.getY();
    }

    @Override
    public void paint(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(lineSize));
        g2d.draw(arc);
    }

    @Override
    public void setBounds(double x, double y, double w, double h) {
        arc.setFrame(x, y, w, h);
        if (h < 0) {
            arc.setAngleStart(180);
            arc.setAngleExtent(-180);
        } else {
            arc.setAngleStart(0);
            arc.setAngleExtent(180);
        }
    }
    @Override
    public void move(double newX, double newY) {
        arc.setFrame(newX, newY, arc.width, arc.height);
    }
    @Override
    public double getWidth() {
        return arc.getWidth();
    }

    @Override
    public double getHeight() {
        return arc.getHeight();
    }

    public void setFlipped(boolean flipped) {
        if(flipped) {
            arc.setAngleStart(180);
            arc.setAngleExtent(-180);
        } else {
            arc.setAngleStart(0);
            arc.setAngleExtent(180);
        }
    }
}

// BrushShape (pędzel)
class BrushShape extends ColoredShape {
    private final Path2D.Double path = new Path2D.Double();

    public BrushShape(double x, double y) {
        path.moveTo(x, y);
    }

    @Override
    public Rectangle2D getBoundingBox() {
        return path.getBounds2D();
    }

    @Override
    public double getX() {
        return 0;
    }

    @Override
    public double getY() {
        return 0;
    }

    @Override
    public void paint(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(lineSize));
        g2d.draw(path);
    }

    @Override
    public void setBounds(double x, double y, double w, double h) {
        // brush jest dynamiczny
    }

    @Override
    public double getWidth() {
        return 0;
    }

    @Override
    public double getHeight() {
        return 0;
    }

    public void addPoint(double x, double y) {
        path.lineTo(x, y);
    }
}

// PolygonShape (wielokąt)
class PolygonShape extends ColoredShape {
    private final Polygon polygon = new Polygon();
    public double lastX;
    public double lastY;

    public void addPoint(int x, int y) {
        polygon.addPoint(x, y);
    }

    public void closePolygon() {
        if (polygon.npoints > 2) {
            polygon.addPoint(polygon.xpoints[0], polygon.ypoints[0]);
        }
    }

    @Override
    public Rectangle2D getBoundingBox() {
        return polygon.getBounds2D();
    }

    @Override
    public double getX() {
        return polygon.getBounds().getX();
    }

    @Override
    public double getY() {
        return polygon.getBounds().getY();
    }

    @Override
    public void paint(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(lineSize));
        g2d.draw(polygon);
    }

    @Override
    public void setBounds(double x, double y, double w, double h) {
    }

    @Override
    public double getWidth() {
        return 0;
    }

    @Override
    public double getHeight() {
        return 0;
    }

}
interface ShapeObserver {
    void onShapeSelected(String shapeName);
}