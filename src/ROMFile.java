import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;

public class ROMFile
{
    private String romName;

    public ROMFile(String name) throws IOException
    {
        romName = name;
    }

    public RandomAccessFile openROM()
    {
        try
        {
            if ((romName.indexOf("smd") > 0) ||
                (romName.indexOf("SMD") > 0))
            {
                return new SMDFile(romName, "rw");
            }
            else
            {
                return new RandomAccessFile(romName, "rw");
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();

            System.out.println("Error opening file: " + romName);
        }

        return null;
    }

    public void fixCheckSum() throws IOException
    {
        RandomAccessFile f = openROM();

        byte buffer[] = new byte[32768];

        f.seek(512);

        int count = 0;

        while (f.getFilePointer() < f.length())
        {
            int read_count = f.read(buffer);

            for (int i = 0; i < read_count; i += 2)
            {
                int num;

                if (buffer[i] < 0)
                {
                    num = buffer[i] + 256;
                }
                else
                {
                    num = buffer[i];
                }

                count += num << 8;

                if ((i + 1) < read_count)
                {
                    if (buffer[i + 1] < 0)
                    {
                        num = buffer[i + 1] + 256;
                    }
                    else
                    {
                        num = buffer[i + 1];
                    }

                    count += num;
                }

                count &= 0xFFFF;
            }
        }

        f.seek(0x18E);

        f.write((count >> 8) & 0xFF);
        f.write(count & 0xFF);

        f.close();
    }

    public void checkROM() throws IOException
    {
        RandomAccessFile f = openROM();
        int index_addr;

        f.seek(0xE46E);

        index_addr = 0;
        index_addr |= f.read() << 24;
        index_addr |= f.read() << 16;
        index_addr |= f.read() << 8;
        index_addr |= f.read();

        if (index_addr != 0x045A80)
        {
            return;
        }

    /*
    if (JOptionPane.showConfirmDialog(this,
				      "This ROM hasn't had it's levels migrated.\n" +
				      "In order to save levels in the ROM safely,\n" +
				      "it is necessary to expand the ROM and save\n" +
				      "the levels at the end of the ROM.  If this\n" +
				      "isn't done, data in the ROM may be overwritten,\n" +
				      "as the levels may not fit.  Do you wish to\n" +
				      "migrate the levels?",
				      "Migrate Levels?",
				      JOptionPane.YES_NO_OPTION) ==
	JOptionPane.NO_OPTION)
      {
	return;
      }
    */

        int i, j;
        int new_idx_addr = (int) f.length();
        byte new_idx[] = new byte[68];

        for (i = 0; i < 68; i++)
            new_idx[i] = 0;

        f.seek(new_idx_addr);
        f.write(new_idx);

        for (i = 0; i < 20; i++)
        {
            int addr;
            int level_inc, stage_inc;

            f.seek(0x9454 + i * 2);
            level_inc = f.read();
            stage_inc = f.read();

            f.seek(index_addr + level_inc * 4 + stage_inc * 2);

            addr = 0;
            addr |= f.read() << 8;
            addr |= f.read();

            SonicReader sr = new SonicReader(this, index_addr + addr);

            long length = sr.readCount();
            byte buf[] = new byte[(int) length];

            f.seek(index_addr + addr);
            f.read(buf);

            f.seek(new_idx_addr + 68 + i * 3200);
            f.write(buf);

            f.seek(new_idx_addr + level_inc * 4 + stage_inc * 2);

            int offset = 68 + i * 3200;

            f.write((offset >> 8) & 0xFF);
            f.write(offset & 0xFF);
        }

        f.seek(0xE46E);

        f.write((new_idx_addr >> 24) & 0xFF);
        f.write((new_idx_addr >> 16) & 0xFF);
        f.write((new_idx_addr >> 8) & 0xFF);
        f.write(new_idx_addr & 0xFF);

        int rom_len = (int) f.length();

        f.seek(0x1A4);

        f.write((rom_len >> 24) & 0xFF);
        f.write((rom_len >> 16) & 0xFF);
        f.write((rom_len >> 8) & 0xFF);
        f.write(rom_len & 0xFF);

        fixCheckSum();

        f.close();
    }

    private int getDataAddr(int level, int increment) throws IOException
    {
        int level_inc;
        int loc_addr;
        int addr = 0;

        RandomAccessFile f = openROM();
        f.seek(0x9454 + level * 2);
        level_inc = f.read();

        loc_addr = 0x42594 + level_inc * 12;

        f.seek(loc_addr + increment);

        addr = 0;
        addr |= f.read() << 24;
        addr |= f.read() << 16;
        addr |= f.read() << 8;
        addr |= f.read();

        f.close();

        return addr;
    }

    public Palette readPalette(int level) throws IOException
    {
        RandomAccessFile f = openROM();
        int colours[] = new int[64];
        int count = 0;
        int pal_inc = (getDataAddr(level, 8) & 0xFF000000) >> 24;
        int pal_addr;

        for (count = 0; count < colours.length; count++)
            colours[count] = 0;

        // Next, get address from the file using pal_inc + 0x2782.
        // Seek to addr, then read palette.

        f.seek(0x2782 + (pal_inc * 8));

        pal_addr = 0;
        pal_addr |= f.read() << 24;
        pal_addr |= f.read() << 16;
        pal_addr |= f.read() << 8;
        pal_addr |= f.read();

        f.seek(pal_addr);

        count = 16;

        while (count < 64)
        {
            colours[count] = 0;
            colours[count] |= f.read() << 8;
            colours[count] |= f.read();

            count++;
        }

        f.close();

        return new Palette(colours);
    }

    public Tile[] readTiles(int level) throws IOException
    {
        LinkedList tiles = new LinkedList();
        SonicReader f = new SonicReader(this, getDataAddr(level, 0) & 0x00FFFFFF);

        int bytes[];
        int byte_idx = 0;
        int buf, byte_buf;

        bytes = new int[8];

        while (true)
        {
            buf = 0;

            byte_buf = f.read();
            if (byte_buf < 0)
            {
                break;
            }
            buf |= byte_buf << 24;
            byte_buf = f.read();
            if (byte_buf < 0)
            {
                break;
            }
            buf |= byte_buf << 16;
            byte_buf = f.read();
            if (byte_buf < 0)
            {
                break;
            }
            buf |= byte_buf << 8;
            byte_buf = f.read();
            if (byte_buf < 0)
            {
                break;
            }
            buf |= byte_buf;

            bytes[byte_idx++] = buf;

            if (byte_idx >= 8)
            {
                Tile t = new Tile(bytes);

                byte_idx = 0;
                bytes = new int[8];

                tiles.add(t);
            }
        }

        Tile tarr[] = new Tile[tiles.size()];

        tiles.toArray(tarr);

        return tarr;
    }


    public Chunk[] readChunks(Tile tiles[],
                              Palette pal,
                              int level) throws IOException
    {
        LinkedList chunks = new LinkedList();
        int bytes[];
        int byte_idx = 0;
        int buf, hi, lo;

        SonicReader f = new SonicReader(this, getDataAddr(level, 4) & 0x00FFFFFF);

        bytes = new int[4];

        while (true)
        {
            buf = 0;

            hi = f.read();
            if (hi < 0)
            {
                break;
            }
            buf |= hi << 8;
            lo = f.read();
            if (lo < 0)
            {
                break;
            }
            buf |= lo;

            bytes[byte_idx++] = buf;

            if (byte_idx >= 4)
            {
                Chunk c = new Chunk(tiles, pal, bytes);

                byte_idx = 0;
                bytes = new int[4];

                chunks.add(c);
            }
        }

        Chunk carr[] = new Chunk[chunks.size()];

        chunks.toArray(carr);

        return carr;
    }

    public Block[] readBlocks(Chunk chunks[], Palette pal, int level) throws IOException
    {
        LinkedList blocks = new LinkedList();
        int bytes[];
        int byte_idx = 0;
        int buf, byte_buf;

        SonicReader f = new SonicReader(this, getDataAddr(level, 8) & 0x00FFFFFF);

        bytes = new int[64];

        while (true)
        {
            buf = 0;

            byte_buf = f.read();
            if (byte_buf < 0)
            {
                break;
            }
            buf |= byte_buf << 8;
            byte_buf = f.read();
            if (byte_buf < 0)
            {
                break;
            }
            buf |= byte_buf;

            bytes[byte_idx++] = buf;

            if (byte_idx >= 64)
            {
                Block b = new Block(chunks, pal, bytes);

                byte_idx = 0;
                bytes = new int[64];

                blocks.add(b);
            }
        }

        Block barr[] = new Block[blocks.size()];

        blocks.toArray(barr);

        return barr;
    }

    public Map readMap(Block blocks[],
                       Sprite sprites[],
                       Palette pal,
                       int level)
        throws IOException
    {
        LinkedList elements = new LinkedList();
        byte bytes[];
        int byte_idx = 0;
        int buf, byte_buf;

        int addr, level_inc, stage_inc;
        int index_addr;

        RandomAccessFile rom = openROM();

        rom.seek(0x9454 + level * 2);
        level_inc = rom.read();
        stage_inc = rom.read();

        rom.seek(0xE46E);

        index_addr = 0;
        index_addr |= rom.read() << 24;
        index_addr |= rom.read() << 16;
        index_addr |= rom.read() << 8;
        index_addr |= rom.read();

        rom.seek(index_addr + level_inc * 4 + stage_inc * 2);

        addr = 0;
        addr |= rom.read() << 8;
        addr |= rom.read();

        rom.close();

        SonicReader f = new SonicReader(this, index_addr + addr);

        int seq[] = f.readAll();

        if (seq == null)
        {
            System.out.println("Argh!  Oh bother...");
        }

        return new Map(blocks, pal, seq);
    }

    public void writeMap(Map map, int level) throws IOException
    {
        SonicWriter sw = new SonicWriter(map.getMap());
        int compressBuf[] = sw.readAll();
        int i;

        int index_addr;
        int level_inc, stage_inc;
        int addr;

        RandomAccessFile f = openROM();

        f.seek(0xE46E);

        index_addr = 0;
        index_addr |= f.read() << 24;
        index_addr |= f.read() << 16;
        index_addr |= f.read() << 8;
        index_addr |= f.read();

        f.seek(0x9454 + level * 2);
        level_inc = f.read();
        stage_inc = f.read();

        f.seek(index_addr + level_inc * 4 + stage_inc * 2);

        addr = 0;
        addr |= f.read() << 8;
        addr |= f.read();

        f.seek(index_addr + addr);

        for (i = 0; i < sw.compressedSize(); i++)
        {
            f.write(compressBuf[i]);
        }

        f.close();

        fixCheckSum();
    }

    public Sprite[] readSprites(int level) throws IOException
    {
        LinkedList sprites = new LinkedList();
        RandomAccessFile f = openROM();
        int level_inc, stage_inc;
        int addr;

        f.seek(0x9454 + level * 2);
        level_inc = f.read();
        stage_inc = f.read();

        f.seek(0xE6800 + level_inc * 4 + stage_inc * 2);

        addr = 0;
        addr |= f.read() << 8;
        addr |= f.read();

        f.seek(0xE6800 + addr);

        while (true)
        {
            int x = f.readUnsignedShort();
            int y = f.readUnsignedShort();
            int num = f.readUnsignedByte();
            int type = f.readUnsignedByte();

            if ((x == 0xFFFF) && (y == 0) && (num == 0) && (type == 0))
            {
                break;
            }

            sprites.add(new Sprite(x, y, num, type));
        }

        Sprite sarr[] = new Sprite[sprites.size()];

        sprites.toArray(sarr);

        return sarr;
    }
}
