import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Vector Graphics Editor");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 1400);

            DrawingPanel drawingPanel = new DrawingPanel();
            ToolBar toolbar = new ToolBar(drawingPanel);
            GroupingToolBar gtb = new GroupingToolBar(drawingPanel);
            GraphicAdapter ga = new GraphicAdapter(drawingPanel);
            MenuBarManager mbm = new MenuBarManager(frame, drawingPanel);
            frame.setJMenuBar(mbm.createMenuBar(drawingPanel));


            drawingPanel.setToolBar(toolbar);
            toolbar.addShapeObserver(drawingPanel);

            frame.add(drawingPanel, BorderLayout.CENTER);
            frame.add(gtb, BorderLayout.SOUTH);
            frame.add(toolbar, BorderLayout.NORTH);

            frame.setVisible(true);
        });
    }
}

class GraphicAdapter {
    private final DrawingPanel drawingPanel;

    public GraphicAdapter(DrawingPanel drawingPanel) {
        this.drawingPanel = drawingPanel;
    }

    public void exportToSVG(String filePath) throws Exception {
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
        Document document = domImpl.createDocument(null, "svg", null);
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

        drawingPanel.paint(svgGenerator);

        try (Writer writer = new FileWriter(filePath)) {
            svgGenerator.stream(writer, true);
        }
    }
}
class MenuBarManager {

    private final JFrame frame;
    private final DrawingPanel drawingPanel;

    public MenuBarManager(JFrame frame, DrawingPanel drawingPanel) {
        this.frame = frame;
        this.drawingPanel = drawingPanel;
    }

    public JMenuBar createMenuBar(DrawingPanel dw) {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem saveToSVG = new JMenuItem("Save as SVG");
        saveToSVG.addActionListener(e -> saveAsSVG());
        fileMenu.add(saveToSVG);

        JMenuItem saveAsPng = new JMenuItem("Save as PNG");
        saveAsPng.addActionListener(e -> saveAsImage(dw,"png"));
        fileMenu.add(saveAsPng);

        JMenuItem saveAsJpeg = new JMenuItem("Save as JPEG");
        saveAsJpeg.addActionListener(e -> saveAsImage(dw,"jpeg"));
        fileMenu.add(saveAsJpeg);

        menuBar.add(fileMenu);

        return menuBar;
    }

    private void saveAsSVG() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save as SVG");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("SVG Files", "svg"));

        int userSelection = fileChooser.showSaveDialog(frame);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.endsWith(".svg")) {
                filePath += ".svg";
            }

            try {
                GraphicAdapter graphicAdapter = new GraphicAdapter(drawingPanel);
                graphicAdapter.exportToSVG(filePath);
                JOptionPane.showMessageDialog(frame, "File saved: " + filePath);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Error saving SVG: " + ex.getMessage());
            }
        }
    }

    public static void saveAsImage(DrawingPanel drawingPanel, String format) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save as " + format.toUpperCase());
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(format.toUpperCase() + " Files", format.toLowerCase()));
        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.endsWith("." + format.toLowerCase())) {
                filePath += "." + format.toLowerCase();
            }
            try {
                BufferedImage image = new BufferedImage(
                        drawingPanel.getWidth(),
                        drawingPanel.getHeight(),
                        format.equalsIgnoreCase("jpeg") ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB
                );
                Graphics2D g2d = image.createGraphics();
                drawingPanel.paint(g2d);
                g2d.dispose();
                File outputFile = new File(filePath);
                boolean success = ImageIO.write(image, format.toLowerCase(), outputFile);

                if (success) {
                    JOptionPane.showMessageDialog(null, "File saved successfully: " + filePath);
                } else {
                    throw new Exception("Unsupported format: " + format);
                }

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Error saving image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }
}
class DrawingPanel extends JPanel implements ShapeObserver{
    private final List<ColoredShape> shapes = new ArrayList<>();
    private ColoredShape currentShape = null;

    private double startX, startY;
    private double offsetX, offsetY;
    private ColoredShape selectedShape = null;
    private final Color currentColor = Color.BLACK;
    private final List<ColoredShape> selectedShapes = new ArrayList<>();


    private ToolBar toolBar;

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
                    if (!e.isShiftDown()) {
                        selectedShapes.clear();
                    }
                    ColoredShape found = findShapeAt(e.getX(), e.getY());
                    if (found != null) {
                        selectedShapes.add(found);
                        offsetX = e.getX() - found.getX();
                        offsetY = e.getY() - found.getY();
                        startX = found.getX();
                        startY = found.getY();
                        selectedShape = found;
                    }
                    repaint();
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
                    double oldX = startX;
                    double oldY = startY;
                    double newX = selectedShape.getX();
                    double newY = selectedShape.getY();
                    commandManager.executeCommand(new MoveCommand(selectedShape, oldX, oldY, newX, newY));

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
    public void groupSelectedShapes() {
        if (selectedShapes.size() < 2) {
            return;
        }
        ShapeGroup group = new ShapeGroup();

        for (ColoredShape s : selectedShapes) {
            group.add(s);
            shapes.remove(s);
        }
        shapes.add(group);
        selectedShapes.clear();
        selectedShapes.add(group);
        repaint();
    }

    public void ungroupSelectedShapes() {
        if (selectedShapes.size() == 1 && selectedShapes.get(0) instanceof ShapeGroup group) {
            List<ColoredShape> children = group.getChildren();
            shapes.addAll(children);
            shapes.remove(group);
            selectedShapes.clear();
            repaint();
        }
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

    private boolean isInsideShape(ColoredShape shape, double px, double py) {
        double x = shape.getX();
        double y = shape.getY();
        double w = shape.getWidth();
        double h = shape.getHeight();
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

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

class ToolBar extends JToolBar {
    private String currentTool = "Rectangle";
    private Color selectedColor = Color.BLACK;
    private int lineSize = 1;
    private final List<ShapeObserver> observers = new ArrayList<>();


    public ToolBar(DrawingPanel drawingPanel) {
        JSlider strokeSlider = new JSlider(1, 21, lineSize);
        strokeSlider.addChangeListener(e -> lineSize = strokeSlider.getValue());
        addButton("Rectangle", () -> currentTool = "Rectangle");
        addButton("Ellipse", () -> currentTool = "Ellipse");
        addButton("Circle", () -> currentTool = "Circle");
        addButton("Polygon", () -> currentTool = "Polygon");
        addButton("Arc", () -> currentTool = "ArcShape");
        addButton("Line", () -> currentTool = "Line");
        addButton("Brush", () -> currentTool = "Brush");
        add(strokeSlider);
        addButton("Move", () -> currentTool = "Move");
        addButton("Undo", drawingPanel::undo);
        addButton("Redo", drawingPanel::redo);

        JButton colorButton = new JButton("Color");
        colorButton.addActionListener(e -> {
            Color c = JColorChooser.showDialog(drawingPanel, "Choose a color", selectedColor);
            if (c != null) selectedColor = c;
        });
        add(colorButton);


    }

    private void addButton(String name, Runnable action) {
        JButton btn = new JButton(name);
        btn.addActionListener(e -> {
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
class GroupingToolBar extends JToolBar {
    public GroupingToolBar(DrawingPanel drawingPanel) {
        setOrientation(HORIZONTAL);
        setFloatable(false);

        JButton groupButton = new JButton("Group");
        groupButton.addActionListener(e -> drawingPanel.groupSelectedShapes());
        add(groupButton);

        JButton ungroupButton = new JButton("Ungroup");
        ungroupButton.addActionListener(e -> drawingPanel.ungroupSelectedShapes());
        add(ungroupButton);
    }
}

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

class ShapeFactory {
    public static ColoredShape createShape(String tool, double x, double y) {
        return switch (tool) {
            case "Rectangle" -> new RectangleBuilder()
                    .setPosition(x, y)
                    .setSize(100, 50)
                    .setColor(Color.BLACK)
                    .setLineSize(2f)
                    .build();
            case "Ellipse" -> new EllipseBuilder()
                    .setPosition(x, y)
                    .setSize(80, 40)
                    .setColor(Color.RED)
                    .build();
            case "Circle" -> new CircleBuilder()
                    .setPosition(x, y)
                    .setSize(50,50)
                    .setColor(Color.BLUE)
                    .setLineSize(3f)
                    .build();
            case "Line" -> new LineBuilder()
                    .setStart(x, y)
                    .setEnd(x + 100, y + 100)
                    .setColor(Color.GREEN)
                    .setLineSize(1.5f)
                    .build();
            case "ArcShape" -> new ArcBuilder()
                    .setPosition(x, y)
                    .setSize(100, 50)
                    .setColor(Color.MAGENTA)
                    .setLineSize(2f)
                    .build();
            case "Brush" -> new BrushBuilder()
                    .setColor(Color.CYAN)
                    .setLineSize(1f)
                    .build();
            default -> null;
        };
    }
}


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

    public void move(double newX, double newY) {
    }

    public abstract double getWidth();
    public abstract double getHeight();

    public void addPoint(int x, int y) {
    }
}
class ShapeGroup extends ColoredShape{
    private final List<ColoredShape> children = new ArrayList<>();
    @Override
    public Rectangle2D getBoundingBox() {
        if(children.isEmpty())
        {
            return new Rectangle2D.Double();
        }
        Rectangle2D bnds = null;
        for(ColoredShape s : children){
            if(bnds == null){
                bnds = s.getBoundingBox();
            }else{
                bnds = bnds.createUnion(s.getBoundingBox());
            }
        }
        return bnds;
    }

    @Override
    public double getX() {
        return getBoundingBox().getX();
    }

    @Override
    public double getY() {
        return getBoundingBox().getY();
    }

    @Override
    public void paint(Graphics2D g2d) {
        for(ColoredShape s : children){
            s.paint(g2d);
        }
    }

    @Override
    public void setBounds(double x, double y, double w, double h) {
        Rectangle2D bounds = getBoundingBox();
        double scaleX = w / bounds.getWidth();
        double scaleY = h / bounds.getHeight();

        for (ColoredShape shape : children) {
            double newX = x + (shape.getX() - bounds.getX()) * scaleX;
            double newY = y + (shape.getY() - bounds.getY()) * scaleY;
            double newWidth = shape.getWidth() * scaleX;
            double newHeight = shape.getHeight() * scaleY;
            shape.setBounds(newX, newY, newWidth, newHeight);
        }
    }
    @Override
    public void move(double newX, double newY) {
        Rectangle2D oldBounds = getBoundingBox();
        double dx = newX - oldBounds.getX();
        double dy = newY - oldBounds.getY();

        for (ColoredShape shape : children) {
            shape.move(shape.getX() + dx, shape.getY() + dy);
        }
    }
    @Override
    public double getWidth() {
        return getBoundingBox().getWidth();
    }

    @Override
    public double getHeight() {
        return getBoundingBox().getHeight();
    }
    public void add(ColoredShape cs){
        children.add(cs);
    }
    public void remove(ColoredShape cs){
        children.remove(cs);
    }
    public List<ColoredShape> getChildren(){
        return children;
    }
}

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



interface ShapeBuilder {
    ShapeBuilder setPosition(double x, double y);
    ShapeBuilder setSize(double width, double height);
    ShapeBuilder setColor(Color color);
    ShapeBuilder setLineSize(float lineSize);
    ColoredShape build();
}


class RectangleBuilder implements ShapeBuilder {
    private double x, y, width, height;
    private Color color = Color.BLACK;
    private float lineSize = 1f;

    @Override
    public ShapeBuilder setPosition(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    @Override
    public ShapeBuilder setSize(double width, double height) {
        this.width = width;
        this.height = height;
        return this;
    }

    @Override
    public ShapeBuilder setColor(Color color) {
        this.color = color;
        return this;
    }

    @Override
    public ShapeBuilder setLineSize(float lineSize) {
        this.lineSize = lineSize;
        return this;
    }

    @Override
    public ColoredShape build() {
        RectangleShape rect = new RectangleShape(x, y, width, height);
        rect.setColor(color);
        rect.setLineSize(lineSize);
        return rect;
    }
}


class EllipseBuilder implements ShapeBuilder {
    private double x, y, width, height;
    private Color color = Color.BLACK;
    private float lineSize = 1f;
    private boolean isCircle = false;

    public EllipseBuilder setCircle(boolean isCircle) {
        this.isCircle = isCircle;
        return this;
    }

    @Override
    public ShapeBuilder setPosition(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    @Override
    public ShapeBuilder setSize(double width, double height) {
        this.width = width;
        this.height = height;
        return this;
    }

    @Override
    public ShapeBuilder setColor(Color color) {
        this.color = color;
        return this;
    }

    @Override
    public ShapeBuilder setLineSize(float lineSize) {
        this.lineSize = lineSize;
        return this;
    }

    @Override
    public ColoredShape build() {
        EllipseShape ellipse = new EllipseShape(x, y, width, height, isCircle);
        ellipse.setColor(color);
        ellipse.setLineSize(lineSize);
        return ellipse;
    }
}
class LineBuilder implements ShapeBuilder {
    private double x1, y1, x2, y2;
    private Color color = Color.BLACK;
    private float lineSize = 1f;

    public LineBuilder setStart(double x, double y) {
        this.x1 = x;
        this.y1 = y;
        return this;
    }

    public LineBuilder setEnd(double x, double y) {
        this.x2 = x;
        this.y2 = y;
        return this;
    }

    @Override
    public ShapeBuilder setPosition(double x, double y) {
        this.x1 = x;
        this.y1 = y;
        this.x2 = x;
        this.y2 = y;
        return this;
    }

    @Override
    public ShapeBuilder setSize(double width, double height) {
        this.x2 = x1 + width;
        this.y2 = y1 + height;
        return this;
    }

    @Override
    public ShapeBuilder setColor(Color color) {
        this.color = color;
        return this;
    }

    @Override
    public ShapeBuilder setLineSize(float lineSize) {
        this.lineSize = lineSize;
        return this;
    }

    @Override
    public ColoredShape build() {
        LineShape line = new LineShape(x1, y1, x2, y2);
        line.setColor(color);
        line.setLineSize(lineSize);
        return line;
    }
}
class ArcBuilder implements ShapeBuilder {
    private double x, y, width, height, startAngle = 0, extent = 180;
    private Color color = Color.BLACK;
    private float lineSize = 1f;

    public ArcBuilder setAngles(double start, double extent) {
        this.startAngle = start;
        this.extent = extent;
        return this;
    }

    @Override
    public ShapeBuilder setPosition(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    @Override
    public ShapeBuilder setSize(double width, double height) {
        this.width = width;
        this.height = height;
        return this;
    }

    @Override
    public ShapeBuilder setColor(Color color) {
        this.color = color;
        return this;
    }

    @Override
    public ShapeBuilder setLineSize(float lineSize) {
        this.lineSize = lineSize;
        return this;
    }

    @Override
    public ColoredShape build() {
        ArcShape arc = new ArcShape(x, y, width, height, startAngle, extent);
        arc.setColor(color);
        arc.setLineSize(lineSize);
        return arc;
    }
}
class BrushBuilder implements ShapeBuilder {
    private final Path2D.Double path = new Path2D.Double();
    private Color color = Color.BLACK;
    private float lineSize = 1f;

    public BrushBuilder addPoint(double x, double y) {
        if (path.getCurrentPoint() == null) {
            path.moveTo(x, y);
        } else {
            path.lineTo(x, y);
        }
        return this;
    }

    @Override
    public ShapeBuilder setPosition(double x, double y) {
        addPoint(x, y);
        return this;
    }

    @Override
    public ShapeBuilder setSize(double width, double height) {
        // Nie dotyczy BrushShape
        return this;
    }

    @Override
    public ShapeBuilder setColor(Color color) {
        this.color = color;
        return this;
    }

    @Override
    public ShapeBuilder setLineSize(float lineSize) {
        this.lineSize = lineSize;
        return this;
    }

    @Override
    public ColoredShape build() {
        BrushShape brush = new BrushShape(0, 0);
        brush.setColor(color);
        brush.setLineSize(lineSize);
        return brush;
    }
}

class CircleBuilder implements ShapeBuilder {
    private double x, y, size;
    private Color color = Color.BLACK;
    private float lineSize = 1f;

    @Override
    public ShapeBuilder setPosition(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    @Override
    public ShapeBuilder setSize(double width, double height) {
        // W przypadku koła szerokość i wysokość muszą być równe
        this.size = Math.min(width, height);
        return this;
    }

    public CircleBuilder setSize(double size) {
        this.size = size;
        return this;
    }

    @Override
    public ShapeBuilder setColor(Color color) {
        this.color = color;
        return this;
    }

    @Override
    public ShapeBuilder setLineSize(float lineSize) {
        this.lineSize = lineSize;
        return this;
    }

    @Override
    public ColoredShape build() {
        EllipseShape circle = new EllipseShape(x, y, size, size, true);
        circle.setColor(color);
        circle.setLineSize(lineSize);
        return circle;
    }
}
