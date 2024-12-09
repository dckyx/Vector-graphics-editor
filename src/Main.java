import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
public class Main {
    // MVC View
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Vector Graphics Editor");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 1400);
            DrawingPanel drawingPanel = new DrawingPanel();
            frame.add(drawingPanel, BorderLayout.CENTER);
            ToolBar toolbar = new ToolBar(drawingPanel);
            frame.add(toolbar, BorderLayout.NORTH);

            frame.setVisible(true);
        });
    }
}
//MVC model
class DrawingPanel extends JPanel {
    private java.util.List<Shape> shapes = new ArrayList<>();
    private Shape currentShape;

    public DrawingPanel() {
        setBackground(Color.WHITE);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                currentShape = new Rectangle2D.Double(e.getX(), e.getY(), 0, 0);
                shapes.add(currentShape);
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (currentShape instanceof Rectangle2D rect) {
                    rect.setFrame(rect.getX(), rect.getY(), e.getX() - rect.getX(), e.getY() - rect.getY());
                    repaint();
                } else if (currentShape instanceof Ellipse2D elipse){
                    elipse.setFrame(elipse.getX(), elipse.getY(), e.getX() - elipse.getX(), e.getY() - elipse.getY());
                    repaint();
                }else{
                    System.out.println("Chuj");
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        for (Shape shape : shapes) {
            g2d.draw(shape);
        }
    }
}

//MVC kontroler
class ToolBar extends JToolBar {
    public ToolBar(DrawingPanel drawingPanel) {
        JButton rectangleButton = new JButton("Rectangle");
        rectangleButton.addActionListener(e -> System.out.println("Rectangle tool selected"));
        JButton ellipseButton = new JButton("Ellipse");
        ellipseButton.addActionListener(e -> System.out.println("Ellipse tool selected"));

        add(rectangleButton);
        add(ellipseButton);
    }
}