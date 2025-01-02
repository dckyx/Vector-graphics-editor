import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

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
    private ToolBar toolBar;
    private Color currentColor = Color.BLACK;

    public DrawingPanel() {
        setBackground(Color.WHITE);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                String tool = toolBar.getCurrentTool();
                switch (tool) {
                    case "Rectangle" -> currentShape = new Prostokat(e.getX(), e.getY(), 0, 0);
                    case "Ellipse" -> currentShape = new EllipseShape(e.getX(), e.getY(), 0, 0);
                    case "Circle" -> currentShape = new EllipseShape(e.getX(), e.getY(), 0, 0);
                    case "łuk" -> currentShape = new LukShape(e.getX(), e.getY(), 0, 0, 0, 90);
                }


                if (currentShape != null) {
                    currentShape.setColor(currentColor); // Ustawienie aktualnego koloru
                    shapes.add(currentShape);
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (currentShape instanceof Prostokat reactangle) {
                    reactangle.setBounds(reactangle.getRectangle().getX(), reactangle.getRectangle().getY(),
                            e.getX() - reactangle.getRectangle().getX(), e.getY() - reactangle.getRectangle().getY());
                } else if (currentShape instanceof EllipseShape ellipse) {
                    if ("Circle".equals(toolBar.getCurrentTool())) {
                        double diameter = Math.min(e.getX() - ellipse.getEllipse().getX(), e.getY() - ellipse.getEllipse().getY());
                        ellipse.setBounds(ellipse.getEllipse().getX(), ellipse.getEllipse().getY(), diameter, diameter);
                    } else {
                        ellipse.setBounds(ellipse.getEllipse().getX(), ellipse.getEllipse().getY(),
                                e.getX() - ellipse.getEllipse().getX(), e.getY() - ellipse.getEllipse().getY());
                    }
                }
                 else if (currentShape instanceof LukShape arc) {
                    arc.setBounds(arc.getArc().getX(), arc.getArc().getY(),
                            e.getX() - arc.getArc().getX(), e.getY() - arc.getArc().getY());
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
    }

    public void undo() {
        if (!shapes.isEmpty()) {
            shapes.remove(shapes.size() - 1);
            repaint();
        }
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

        JButton lukButton = new JButton("Łuk");
        lukButton.addActionListener(e -> {
            currentTool = "łuk";
            System.out.println("Circle tool selected");
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
        add(colorButton);
        add(undoButton);
    }

    public String getCurrentTool() {
        return currentTool;
    }

    public Color getCurrentColor() {
        return selectedColor;
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

    public abstract void paint(Graphics2D g2d);
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

    public Arc2D getArc() {
        return arc;
    }
}