import javax.swing.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.awt.*;
import javax.swing.border.*;

class BlockPanel extends JPanel implements MouseListener
{
  AffineTransform transform;
  Block blocks[];	 
  int block_num;
  boolean selected;

  ActionListener actionListener = null;

  public BlockPanel(Block b[], int num)
  {
    super();

    transform = new AffineTransform();
    transform.scale(0.5, 0.5);

    setBorder(BorderFactory.createLoweredBevelBorder());

    blocks = b;
    block_num = num;

    addMouseListener(this);
  }

  public void paintComponent(Graphics g)
  {
    Border b = getBorder();
    BufferedImage scaled = new BufferedImage(64, 64, 
					     BufferedImage.TYPE_INT_ARGB);
    Graphics2D gfx = scaled.createGraphics();

    gfx.drawImage(blocks[block_num].getImage(), transform, this);

    if (selected)
      {
	  BufferedImage overlay = new BufferedImage(64, 64, 
						    BufferedImage.TYPE_INT_ARGB);
	  Graphics2D gfx_o = overlay.createGraphics();

	  gfx_o.setColor(new Color((float) 0.0, 
				   (float) 0.0, 
				   (float) 1.0, 
				   (float) 0.5));
	  gfx_o.fill(new Rectangle(0, 0, 64, 64));

	  gfx.drawImage(overlay, 0, 0, this);
      }

    g.drawImage(scaled,
		b.getBorderInsets(this).left, 
		b.getBorderInsets(this).top, 
		this);
  }

  public void unselect()
  {
    selected = false;
    repaint();
  }

  public Dimension getMinimumSize()
  {
    return new Dimension (64, 64);
  }

  public Dimension getPreferredSize()
  {
    return new Dimension (64, 64);
  }

  public void addActionListener(ActionListener l)
  {
    actionListener = AWTEventMulticaster.add(actionListener, l);
  }
      
  public void removeActionListener(ActionListener l)
  {
    actionListener = AWTEventMulticaster.remove(actionListener, l);
  }

  public void mouseClicked(MouseEvent e)
  {
    ActionEvent action = new ActionEvent(this, 
					 ActionEvent.ACTION_PERFORMED,
					 Integer.toString(block_num));

    actionListener.actionPerformed(action);

    selected = true;
    repaint();
  }

  public void mouseEntered(MouseEvent e)
  {
  }

  public void mouseExited(MouseEvent e)
  {
  }

  public void mousePressed(MouseEvent e)
  {
  }

  public void mouseReleased(MouseEvent e)
  {
  }
}

public class BlockSelector extends JScrollPane implements ActionListener
{

  Block blocks[];
  BlockPanel panels[];

  int cur_block;
  ActionListener actionListener = null;

  public BlockSelector()
  {
    super();
  }

  public void setBlocks(Block b[])
  {
    JPanel blockPanel = new JPanel();
    JViewport view = new JViewport();    
    int i;

    panels = new BlockPanel[b.length];
    blocks = b;

    blockPanel.setLayout(new GridLayout(b.length, 1));

    for (i = 0; i < b.length; i++)
      {
        panels[i] = new BlockPanel(b, i);
	blockPanel.add(panels[i]);
	panels[i].setVisible(true);
	panels[i].addActionListener(this);
      }

    view.add(blockPanel);
    blockPanel.setVisible(true);

    setViewport(view);
    validate();
  }

  public Block[] getBlocks() { return blocks; }

  public void addActionListener(ActionListener l)
  {
    actionListener = AWTEventMulticaster.add(actionListener, l);
  }

  public void removeActionListener(ActionListener l)
  {
    actionListener = AWTEventMulticaster.remove(actionListener, l);
  }

  public void actionPerformed(ActionEvent e)
  {
    ActionEvent action = new ActionEvent(this, 
					 ActionEvent.ACTION_PERFORMED,
					 e.getActionCommand());

    actionListener.actionPerformed(action);

    panels[cur_block].unselect();

    try
      {
	cur_block = Integer.parseInt(e.getActionCommand());
      }
    catch (Exception ex)
      {
      }
  }
}
