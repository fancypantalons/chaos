import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class MainWin extends JFrame implements ActionListener
{
    ROMFile rom;
    int level = 0;

    MapCanvas mapCanvas;
    BlockSelector blockSelector;

    JComboBox levelSelector;
    JScrollPane scrollPane;

    public MainWin(String rn) throws IOException
    {
        super("Chaos");

        Container pane = getContentPane();
        Box hbox = Box.createHorizontalBox();
        JPanel mainPane = new JPanel();
        Box controlBox = Box.createVerticalBox();

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        levelSelector = new JComboBox();
        levelSelector.addActionListener(this);
        blockSelector = new BlockSelector();
        scrollPane = new JScrollPane();

        pane.setLayout(new BorderLayout());
        pane.add(menuBar, BorderLayout.NORTH);
        pane.add(mainPane, BorderLayout.CENTER);

        mainPane.setLayout(new BorderLayout());

        mainPane.add(scrollPane, BorderLayout.CENTER);
        mainPane.add(controlBox, BorderLayout.WEST);
        controlBox.add(hbox);

        hbox.add(new JLabel("Level: "));
        hbox.add(levelSelector);

        levelSelector.addItem(" ");

        controlBox.add(blockSelector);
        blockSelector.addActionListener(this);

        menuBar.add(fileMenu);
        fileMenu.add(createMenuItem("Load Levels"));
        fileMenu.add(createMenuItem("Save Level"));
        fileMenu.add(createMenuItem("Quit"));

        //    getLayeredPane().setLayout(new FlowLayout());

        setSize(640, 480);

        rom = new ROMFile(rn);

        if (rn != null)
        {
            loadDataInfo(rom);

            rom.checkROM();
            loadLevel(rom, level);
        }
    }

    void loadDataInfo(ROMFile rom) throws IOException
    {
        int i;

        levelSelector.removeAllItems();

        for (i = 0; i < 20; i++)
        {
            levelSelector.addItem(Integer.toString(i + 1));
        }
    }

    void loadLevel(ROMFile rom, int l_num) throws IOException
    {
        level = l_num;

        Palette pal = null;
        Tile[] tiles = null;
        Chunk[] chunks = null;
        Block[] blocks = null;
        Sprite[] sprites = null;

        try
        {
            pal = rom.readPalette(level);
            tiles = rom.readTiles(level);
            chunks = rom.readChunks(tiles, pal, level);
            blocks = rom.readBlocks(chunks, pal, level);
            sprites = rom.readSprites(level);
        }
        catch (IOException e)
        {
            System.err.println("Error loading level: ");
            e.printStackTrace();
            return;
        }

        Map levelMap = rom.readMap(blocks, sprites, pal, level);

        if (mapCanvas != null)
        {
            mapCanvas.setMap(levelMap);
            mapCanvas.setSprites(sprites);
        }
        else
        {
            mapCanvas = new MapCanvas(levelMap, sprites);
        }

        JViewport view = new JViewport();

        view.add(mapCanvas);
        scrollPane.setViewport(view);
        mapCanvas.setVisible(true);

        blockSelector.setBlocks(blocks);

        getContentPane().validate();
    }

    void saveLevel(ROMFile rom, int l_num) throws IOException
    {

    }

    JMenuItem createMenuItem(String name)
    {
        JMenuItem item = new JMenuItem(name);

        item.addActionListener(this);
        return item;
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == blockSelector)
        {
            if (e.getActionCommand() != "clicked")
            {
                try
                {
                    // levelMap.setBlock(Integer.parseInt(e.getActionCommand()));
                }
                catch (Exception ex)
                {
                }
            }
            else
            {
            }
        }
        else if (e.getActionCommand() == "Load Levels")
        {
            JFileChooser f = new JFileChooser(".");
            int result = f.showOpenDialog(this);

            if ((result == JFileChooser.ERROR_OPTION) ||
                (result == JFileChooser.CANCEL_OPTION))
            {
                return;
            }

            try
            {
                String rn = f.getSelectedFile().getAbsolutePath();

                rom = new ROMFile(rn);

                loadDataInfo(rom);

                rom.checkROM();
                loadLevel(rom, level);
            }
            catch (IOException ex)
            {
            }
        }
        else if (e.getActionCommand() == "Save Level")
        {
            try
            {
                saveLevel(rom, level);
            }
            catch (IOException ex)
            {
            }
        }
        else if (e.getActionCommand() == "Quit")
        {
            System.exit(0);
        }
        else if (e.getSource() == levelSelector)
        {
            String level_str = (String) levelSelector.getSelectedItem();

            try
            {
                loadLevel(rom, Integer.parseInt(level_str) - 1);
            }
            catch (Exception ex)
            {
            }
        }
    }
}
