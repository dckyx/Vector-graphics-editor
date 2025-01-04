import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Vector Graphics Editor");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 1400);
            DrawingPanel drawingPanel = new DrawingPanel();
            ToolBar toolbar = new ToolBar(drawingPanel);
            drawingPanel.setToolBar(toolbar);

            frame.add(drawingPanel, BorderLayout.CENTER);
            frame.add(toolbar, BorderLayout.NORTH);

            frame.setVisible(true);
        });
    }
}

// MVC model
class DrawingPanel extends JPanel {
    private java.util.List<ColoredShape> shapes = new ArrayList<>();
    private ColoredShape currentShape;
    private double startX, startY;
    private ToolBar toolBar;
    private Color currentColor = Color.BLACK;
    private final CommandManager cM = new CommandManager();
    private final Stack<Command> undoStack = new Stack<>();
    private final Stack<Command> redoStack = new Stack<>();

    public DrawingPanel() {
        setBackground(Color.WHITE);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                startX = e.getX();
                startY = e.getY();

                String tool = toolBar.getCurrentTool();
                switch (tool) {
                    case "Rectangle" -> currentShape = new Prostokat(startX, startY, 0, 0);
                    case "Ellipse"   -> currentShape = new EllipseShape(startX, startY, 0, 0);
                    case "Circle"    -> currentShape = new EllipseShape(startX, startY, 0, 0);
                    case "łuk"       -> currentShape = new LukShape(startX, startY, 0, 0, 0, 90);
                    case "Linia"     -> currentShape = new Linia(startX,startY,0,0);
                    case "Brush"     -> currentShape = new BrushShape(startX,startY);
                    default          -> currentShape = null;
                }

                if (currentShape != null) {
                    currentShape.setColor(currentColor);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (currentShape != null) {
                    cM.executeCommand(new AddShapeCommand(DrawingPanel.this, currentShape));
                    currentShape = null;
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (currentShape == null) return;

                double currentX = e.getX();
                double currentY = e.getY();
                double width  = Math.abs(currentX - startX);
                double height = Math.abs(currentY - startY);
                double newX = Math.min(startX, currentX);
                double newY = Math.min(startY, currentY);
                if (currentShape instanceof Prostokat rect) {
                    rect.setBounds(newX, newY, width, height);
                } else if (currentShape instanceof EllipseShape ellipse) {
                    if ("Circle".equals(toolBar.getCurrentTool())) {
                        double diameter = Math.min(width, height);
                        ellipse.setBounds(newX, newY, diameter, diameter);
                    } else {
                        ellipse.setBounds(newX, newY, width, height);
                    }
                } else if (currentShape instanceof LukShape arc) {
                    arc.setBounds(newX, newY, width, height);
                } else if (currentShape instanceof Linia linia){
                    linia.setBounds(newX, newY, width, height);
                } else if(currentShape instanceof BrushShape brush){
                    brush.addPoint(e.getX(), e.getY());
                    repaint();
                }

                repaint();
            }
        });
    }
    public void setToolBar(ToolBar toolBar) {
        this.toolBar = toolBar;
    }

    public void setColor(Color color) {
        this.currentColor = color;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        for (ColoredShape shape : shapes) {
            shape.paint(g2d);
        }
        if(currentShape != null){
            currentShape.paint(g2d);
        }
    }
    public void addShape(ColoredShape shape) {
        shapes.add(shape);
        repaint();
    }

    public void removeShape(ColoredShape shape) {
        shapes.remove(shape);
        repaint();
    }

    public void undo(){
        cM.undo();
        repaint();
    }
    public void redo(){
        cM.redo();
        repaint();
    }
}
// MVC kontroler
class ToolBar extends JToolBar {
    private String currentTool = "Rectangle";
    private Color selectedColor = Color.BLACK;

    public ToolBar(DrawingPanel drawingPanel) {
        JButton rectangleButton = new JButton("Rectangle");
        rectangleButton.addActionListener(e -> {
            currentTool = "Rectangle";
            System.out.println("Rectangle tool selected");
        });

        JButton ellipseButton = new JButton("Ellipse");
        ellipseButton.addActionListener(e -> {
            currentTool = "Ellipse";
            System.out.println("Ellipse tool selected");
        });

        JButton circleButton = new JButton("Circle");
        circleButton.addActionListener(e -> {
            currentTool = "Circle";
            System.out.println("Circle tool selected");
        });

        JButton undoButton = new JButton("Undo");
        undoButton.addActionListener(e -> drawingPanel.undo());

        JButton redoButton = new JButton("Redo");
        redoButton.addActionListener(e-> drawingPanel.redo());

        JButton lukButton = new JButton("Łuk");
        lukButton.addActionListener(e -> {
            currentTool = "łuk";
            System.out.println("Circle tool selected");
        });

        JButton liniaButton = new JButton("Linia");
        liniaButton.addActionListener(e->{
            currentTool = "Linia";
            System.out.println("Line tool selected");
        });

        JButton brushButton = new JButton("Brush");
        brushButton.addActionListener(e->{
            currentTool = "Brush";
            System.out.println("Brush tool selected");
        });
        JButton colorButton = new JButton("Color");
        colorButton.addActionListener(e -> {
            Color color = JColorChooser.showDialog(drawingPanel, "Choose a color", selectedColor);
            if (color != null) {
                selectedColor = color;
                drawingPanel.setColor(color);
            }
        });

        add(rectangleButton);
        add(ellipseButton);
        add(circleButton);
        add(lukButton);
        add(liniaButton);
        add(brushButton);
        add(colorButton);
        add(undoButton);
        add(redoButton);
    }

    public String getCurrentTool() {
        return currentTool;
    }

    public Color getCurrentColor() {
        return selectedColor;
    }
}

interface Command{
    void execute();
    void undo();
    void redo();
}
class AddShapeCommand implements Command{
    private final DrawingPanel panel;
    private final ColoredShape shape;
    public AddShapeCommand(DrawingPanel panel, ColoredShape shape){
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

class MoveCommand implements Command{
    private final ColoredShape shape;
    private final double oldX, oldY;
    private final double newX, newY;

    public MoveCommand(ColoredShape shape, double oldX, double oldY, double newX, double newY){
        this.shape = shape;
        this.oldX = oldX;
        this.oldY = oldY;
        this.newX = newX;
        this.newY = newY;
    }

    @Override
    public void execute() {
        shape.setBounds(newX, newY,shape.getWidth(),shape.getHeight());
    }

    @Override
    public void undo() {
        shape.setBounds(oldX, oldY,shape.getWidth(), shape.getHeight());
    }

    @Override
    public void redo() {
        execute();
    }
}
class CommandManager{
    private Stack<Command> redoStack = new Stack<>();
    private Stack<Command> undoStack = new Stack<>();
    void executeCommand(Command cmd){
        cmd.execute();
        undoStack.push(cmd);
        redoStack.clear();
    }
    void undo(){
        if(!undoStack.isEmpty()){
            Command cmd = undoStack.pop();
            cmd.undo();
            redoStack.push(cmd);
        }
    }
    void redo(){
        if(!redoStack.isEmpty()){
            Command cmd = redoStack.pop();
            cmd.redo();
            undoStack.push(cmd);
        }
    }
}
// Abstrakcyjna klasa ColoredShape
abstract class ColoredShape {
    protected Color color;

    public ColoredShape() {
        this.color = Color.BLACK;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
    public abstract void setBounds(double x, double y, double width, double height);

    public abstract void paint(Graphics2D g2d);
    public abstract double getWidth();
    public abstract double getHeight();
    public abstract double getX();
    public abstract double getY();

}
// Klasa Prostokat
class Prostokat extends ColoredShape {
    private Rectangle2D rectangle;

    public Prostokat(double x, double y, double width, double height) {
        rectangle = new Rectangle2D.Double(x, y, width, height);
    }

    public void setBounds(double x, double y, double width, double height) {
        rectangle.setFrame(x, y, width, height);
    }

    @Override
    public void paint(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.draw(rectangle);
    }

    @Override
    public double getWidth() {
        return this.rectangle.getWidth();
    }

    @Override
    public double getHeight() {
        return this.rectangle.getHeight();
    }

    @Override
    public double getX() {
        return rectangle.getX();
    }

    @Override
    public double getY() {
        return rectangle.getY();
    }

    public Rectangle2D getRectangle() {
        return rectangle;
    }
}

// Klasa EllipseShape
class EllipseShape extends ColoredShape {
    private Ellipse2D ellipse;

    public EllipseShape(double x, double y, double width, double height) {
        ellipse = new Ellipse2D.Double(x, y, width, height);
    }

    public void setBounds(double x, double y, double width, double height) {
        ellipse.setFrame(x, y, width, height);
    }

    @Override
    public void paint(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.draw(ellipse);
    }

    @Override
    public double getWidth() {
        return this.ellipse.getWidth();
    }

    @Override
    public double getHeight() {
        return this.ellipse.getHeight();
    }
    @Override
    public double getX() {
        return ellipse.getX();
    }

    @Override
    public double getY() {
        return ellipse.getY();
    }

    public Ellipse2D getEllipse() {
        return ellipse;
    }
}
class LukShape extends ColoredShape {
    private Arc2D arc;

    public LukShape(double x, double y, double width, double height, double start, double extent) {
        arc = new Arc2D.Double(x, y, width, height, start, extent, Arc2D.OPEN);
    }

    public void setBounds(double x, double y, double width, double height) {
        arc.setFrame(x, y, width, height);
    }

    @Override
    public void paint(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.draw(arc);
    }

    @Override
    public double getWidth() {
        return this.arc.getWidth();
    }

    @Override
    public double getHeight() {
        return this.arc.getHeight();
    }
    @Override
    public double getX() {
        return arc.getX();
    }

    @Override
    public double getY() {
        return arc.getY();
    }

    public Arc2D getArc() {
        return arc;
    }
}

class Linia extends ColoredShape {
    private Line2D line;

    public Linia(double x, double y, double width, double height) {
        line = new Line2D.Double(x, y, x + width, y + height);
    }

    @Override
    public void setBounds(double x, double y, double width, double height) {
        line.setLine(x, y, x + width, y + height);
    }

    @Override
    public void paint(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.draw(line);
    }

    @Override
    public double getWidth() {
        return Math.abs(line.getX2() - line.getX1());
    }

    @Override
    public double getHeight() {
        return Math.abs(line.getY2() - line.getY1());
    }

    @Override
    public double getX() {
        return Math.min(line.getX1(), line.getX2());
    }

    @Override
    public double getY() {
        return Math.min(line.getY1(), line.getY2());
    }

    public Line2D getLine() {
        return line;
    }
}
class BrushShape extends ColoredShape {
    private Path2D path;

    public BrushShape(double startX, double startY) {
        path = new Path2D.Double();
        path.moveTo(startX, startY);
    }

    @Override
    public void setBounds(double x, double y, double width, double height) {
    }


    public void addPoint(double x, double y) {
        path.lineTo(x, y);
    }

    @Override
    public void paint(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.draw(path);
    }

    @Override
    public double getWidth() {
        return path.getBounds2D().getWidth();
    }

    @Override
    public double getHeight() {
        return path.getBounds2D().getHeight();
    }

    @Override
    public double getX() {
        return path.getBounds2D().getX();
    }

    @Override
    public double getY() {
        return path.getBounds2D().getY();
    }
}
