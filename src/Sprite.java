import java.awt.image.*;
import java.awt.*;

public class Sprite
{
  int x, y;
  int num;
  int type;

  public Sprite(int xp, int yp, int n, int t)
  {
    x = xp;
    y = yp;
    num = n;
    type = t;

    y &= 0x0FFF; // Strip off flags for now
  }

  public void paint(Graphics g)
  {
    g.setColor(new Color(255, 255, 255));

    g.drawString(Integer.toString(num) + " " + Integer.toString(type), 
                 x, 
                 y);
  }

  public int getX() { return x; };
  public int getY() { return y; };
}
