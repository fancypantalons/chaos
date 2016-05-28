/*
  Java-based Tile Viewer.  Supports separate tile/palette files, as well
  as the Sonic the Hedgehog 2 ROM... ie, it can extract palettes from the
  ROM.
*/

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.RandomAccessFile;

class TilePanel extends JPanel
{
    Tile myTile;
    Palette pal;
    int pal_idx;

    AffineTransform transform;

    public TilePanel()
    {
        setBorder(BorderFactory.createLoweredBevelBorder());
        pal_idx = 0;

        transform = new AffineTransform();
        transform.scale(4, 4);
    }

    public void setTile(Tile t)
    {
        myTile = t;
    }

    public void setPalette(Palette p)
    {
        pal = p;
    }

    public void setPalIdx(int idx)
    {
        pal_idx = idx;
    }

    public void paintComponent(Graphics g)
    {
        if ((myTile == null) || (pal == null))
        {
            return;
        }

        BufferedImage img;
        BufferedImage scaled = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Border b = getBorder();

        img = myTile.getImage(pal.getPalette(), pal_idx, false, false);
        scaled.createGraphics().drawImage(img, transform, this);

        g.drawImage(
            scaled,
            b.getBorderInsets(this).left,
            b.getBorderInsets(this).top,
            this
        );
    }

    public Dimension getMinimumSize()
    {
        return new Dimension(32, 32);
    }

    public Dimension getPreferredSize()
    {
        return new Dimension(32, 32);
    }
}

class TileFrame extends JFrame implements KeyListener
{
    static final int width = 16;
    static final int height = 16;

    RandomAccessFile file;
    long offset;
    JLabel locLabel;
    JLabel levelLabel;
    JLabel palLabel;

    TilePanel panels[];

    Palette rom_pal;
    Palette work_pal;

    int pal_idx;
    int level;

    boolean fixed_pal;

    public TileFrame(String fname)
    {
        level = 0;

        try
        {
            ROMFile rom = new ROMFile(fname);

            fixed_pal = false;

            file = rom.openROM();
            rom_pal = readPalette(file, level);
            work_pal = rom_pal;
            pal_idx = 0;
        }
        catch (IOException e)
        {
            System.out.println("Error opening file: " + e.getMessage());
            System.exit(0);
        }

        buildGUI();
    }

    public TileFrame(String fname, String pal_name)
    {
        level = 0;

        try
        {
            ROMFile rom = new ROMFile(fname);

            fixed_pal = true;

            file = rom.openROM();
            rom_pal = readRawPalette(new RandomAccessFile(pal_name, "r"));
            work_pal = rom_pal;
            pal_idx = 0;
        }
        catch (IOException e)
        {
            System.out.println("Error opening file: " + e.getMessage());
            System.exit(0);
        }

        buildGUI();
    }

    void buildGUI()
    {
        getContentPane().setLayout(new BorderLayout());

        offset = 0;

        JPanel lPanel = new JPanel();

        lPanel.setLayout(new GridLayout(3, 1));
        getContentPane().add(lPanel, BorderLayout.NORTH);

        locLabel = new JLabel("Location: 0");
        lPanel.add(locLabel);

        levelLabel = new JLabel("Level: 0");

        if (!fixed_pal)
        {
            lPanel.add(levelLabel);
        }

        palLabel = new JLabel("Palette: 0");
        lPanel.add(palLabel);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(width, height));

        panels = new TilePanel[width * height];

        for (int i = 0; i < panels.length; i++)
        {
            panels[i] = new TilePanel();
            mainPanel.add(panels[i]);
        }

        getContentPane().add(mainPanel, BorderLayout.CENTER);

        addKeyListener(this);

        loadTiles(offset);

        pack();
    }

    int getDataAddr(RandomAccessFile f, int level, int increment) throws IOException
    {
        int level_inc;
        int loc_addr;
        int addr;

        f.seek(0x9454 + level * 2);
        level_inc = f.read();
        loc_addr = 0x42594 + level_inc * 12;

        f.seek(loc_addr + increment);

        addr = 0;
        addr |= f.read() << 24;
        addr |= f.read() << 16;
        addr |= f.read() << 8;
        addr |= f.read();

        return addr;
    }

    Palette readPalette(RandomAccessFile f, int level) throws IOException
    {
        int colours[] = new int[64];
        int count = 0;
        int pal_addr;
        int pal_inc = 0;

        if (!fixed_pal)
        {
            pal_inc = (getDataAddr(f, level, 8) & 0xFF000000) >> 24;
        }

        for (count = 0; count < colours.length; count++)
            colours[count] = 0;

        // Next, get address from the file using pal_inc + 0x2782.
        // Seek to addr, then read palette.

        if (!fixed_pal)
        {
            f.seek(0x2782 + (pal_inc * 8));

            pal_addr = 0;
            pal_addr |= f.read() << 24;
            pal_addr |= f.read() << 16;
            pal_addr |= f.read() << 8;
            pal_addr |= f.read();

            f.seek(pal_addr);

            count = 16;
        }
        else
        {
            count = 0;
        }

        while (count < 64)
        {
            colours[count] = 0;

            if (!fixed_pal)
            {
                colours[count] |= f.read() << 8;
                colours[count] |= f.read();
            }
            else
            {
                colours[count] |= f.read();
                colours[count] |= f.read() << 8;
            }

            count++;
        }

        return new Palette(colours);
    }

    Palette readRawPalette(RandomAccessFile f) throws IOException
    {
        int count = 0;
        int colours[] = new int[64];

        while (count < 64)
        {
            colours[count] = 0;
            colours[count] |= f.read() << 8;
            colours[count] |= f.read();

            count++;
        }

        return new Palette(colours);
    }

    void setPalette(Palette p, int idx)
    {
        for (int i = 0; i < panels.length; i++)
        {
            panels[i].setPalette(p);
            panels[i].setPalIdx(idx);
            panels[i].repaint();
        }
    }

    void loadTiles(long offset)
    {
        try
        {
            file.seek(offset);

            for (int i = 0; i < panels.length; i++)
            {
                int bytes[] = new int[8];

                for (int j = 0; j < bytes.length; j++)
                    bytes[j] = file.readInt();

                panels[i].setPalette(work_pal);
                panels[i].setPalIdx(pal_idx);
                panels[i].setTile(new Tile(bytes));
                panels[i].repaint();

                panels[i].setToolTipText(
                    "Position: " +
                        Long.toString(file.getFilePointer() - 32)
                );
            }
        }
        catch (IOException e)
        {
            System.out.println("Error reading data from file!");
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    public void keyPressed(KeyEvent e)
    {
        if (e.getKeyCode() == KeyEvent.VK_DOWN)
        {
            offset += (width * height * 32);
        }
        else if (e.getKeyCode() == KeyEvent.VK_UP)
        {
            offset -= (width * height * 32);
        }
        else if (e.getKeyCode() == KeyEvent.VK_PAGE_DOWN)
        {
            offset += (width * height * 32) * 4;
        }
        else if (e.getKeyCode() == KeyEvent.VK_PAGE_UP)
        {
            offset -= (width * height * 32) * 4;
        }
        else if (e.getKeyCode() == KeyEvent.VK_RIGHT)
        {
            offset += 1;
        }
        else if (e.getKeyCode() == KeyEvent.VK_LEFT)
        {
            offset -= 1;
        }
        else if (e.getKeyCode() == KeyEvent.VK_HOME)
        {
            offset = 0;
        }
        else if (e.getKeyCode() == KeyEvent.VK_END)
        {
            try
            {
                offset = file.length() - (width * height * 32);
            }
            catch (IOException ex)
            {
                System.out.println("Error getting file length!");
                System.exit(0);
            }
        }
        else if (e.getKeyCode() == KeyEvent.VK_ADD)
        {
            pal_idx++;

            if (pal_idx > 3)
            {
                pal_idx = 3;
            }

            setPalette(work_pal, pal_idx);
            palLabel.setText("Palette: " + pal_idx);
        }
        else if (e.getKeyCode() == KeyEvent.VK_SUBTRACT)
        {
            pal_idx--;

            if (pal_idx < 0)
            {
                pal_idx = 0;
            }

            setPalette(work_pal, pal_idx);
            palLabel.setText("Palette: " + pal_idx);
        }
        else if (e.getKeyCode() == KeyEvent.VK_G)
        {
            int colours[] = new int[64];

            for (int i = 0; i < 16; i++)
            {
                int c = 0x0000 |
                    ((i << 8) & 0x0F00) |
                    ((i << 4) & 0x00F0) |
                    i & 0x000F;

                colours[i] = c;
                colours[i + 16] = c;
                colours[i + 32] = c;
                colours[i + 48] = c;
            }

            work_pal = new Palette(colours);

            setPalette(work_pal, pal_idx);
        }
        else if (e.getKeyCode() == KeyEvent.VK_C)
        {
            work_pal = rom_pal;
            setPalette(rom_pal, pal_idx);
        }
        else if ((e.getKeyCode() == KeyEvent.VK_N) && (!fixed_pal))
        {
            level++;

            if (level > 19)
            {
                level = 19;
            }

            try
            {
                rom_pal = readPalette(file, level);
            }
            catch (IOException ex)
            {
                System.out.println("Error reading palette for level: " + level);
                System.exit(0);
            }

            work_pal = rom_pal;
            setPalette(rom_pal, pal_idx);

            levelLabel.setText("Level: " + level);
        }
        else if ((e.getKeyCode() == KeyEvent.VK_P) && (!fixed_pal))
        {
            level--;

            if (level < 0)
            {
                level = 0;
            }

            try
            {
                rom_pal = readPalette(file, level);
            }
            catch (IOException ex)
            {
                System.out.println("Error reading palette for level: " + level);
                System.exit(0);
            }

            work_pal = rom_pal;
            setPalette(rom_pal, pal_idx);

            levelLabel.setText("Level: " + level);
        }
        else if (e.getKeyCode() == KeyEvent.VK_Q)
        {
            System.exit(0);
        }

        try
        {
            if (offset < 0)
            {
                offset = 0;
            }
            if (offset + (width * height * 32) >= file.length())
            {
                offset = file.length() - (width * height * 32);
            }

            loadTiles(offset);
        }
        catch (IOException ex)
        {
            System.out.println("Error moving data pointer!");
            System.exit(0);
        }

        locLabel.setText("Location: " + Long.toString(offset));
    }

    public void keyReleased(KeyEvent e)
    {
    }

    public void keyTyped(KeyEvent e)
    {
    }
}

public class TileViewer
{
    public static void main(String params[]) throws Exception
    {
        JFrame frame = null;

        if (params.length == 1)
        {
            frame = new TileFrame(params[0]);
        }
        else if (params.length == 2)
        {
            frame = new TileFrame(params[0], params[1]);
        }
        else
        {
            System.out.println("TileViewer [tile_file] <palette file>");
        }

        if (frame != null)
        {
            frame.show();
        }
    }
}
