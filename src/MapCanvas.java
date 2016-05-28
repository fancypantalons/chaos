import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class MapCanvas extends JPanel
{
    Map map;
    Sprite[] sprites;

    public MapCanvas(Map m, Sprite[] s)
    {
        super();

        map = m;
        sprites = s;
    }

    public void setMap(Map m)
    {
        map = m;
    }

    public void setSprites(Sprite[] s)
    {
        sprites = s;
    }

    public void paintComponent(Graphics g)
    {
        Rectangle r = g.getClipBounds();
        BufferedImage image = map.getImage(r.x, r.y, r.width, r.height);

        g.drawImage(image, r.x, r.y, r.width, r.height, this);
    }

    public Dimension getPreferredSize()
    {
        return new Dimension(128 * 128, 128 * 16);
    }

    public Dimension getMinimumSize()
    {
        return new Dimension(128 * 128, 128 * 16);
    }
}
