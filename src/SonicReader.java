import java.io.IOException;
import java.io.RandomAccessFile;

class SonicReader
{
    RandomAccessFile file;
    int data_buffer[];

    int bit_field;
    int bit_count;

    int read_pos;
    int write_pos;
    long read_size;
    boolean done;

    public SonicReader(ROMFile rom, int offset) throws IOException
    {
        file = rom.openROM();

        data_buffer = new int[65535 * 2];

        file.seek(offset);
        load_bitfield();
        write_pos = 0;
        read_size = 0;

        load();
    }

    void load_bitfield()
    {
        try
        {
            bit_field = 0;
            bit_field |= file.read();
            bit_field |= file.read() << 8;

            bit_count = 15;
        }
        catch (IOException e)
        {
            done = true;
        }
    }

    int get_bit()
    {
        int bit = bit_field & 1;

        bit_field >>= 1;
        bit_count--;

        if (bit_count < 0)
        {
            load_bitfield();
        }

        return bit;
    }

    void load()
    {
        int count;
        int offset;
        long start_pos = 0;

        try
        {
            start_pos = file.getFilePointer();
        }
        catch (IOException e)
        {
        }

        while (!done)
        {
            if (get_bit() != 0)
            {
                try
                {
                    data_buffer[write_pos++] = file.read();
                }
                catch (IOException e)
                {
                    done = true;
                }

                continue;
            }
            else
            {
                if (get_bit() != 0)
                {
                    int low, high;

                    try
                    {
                        low = file.read();
                        high = file.read();
                    }
                    catch (IOException e)
                    {
                        done = true;
                        continue;
                    }

                    offset = (0xFFFFFF00 | high) << 5;
                    offset = (offset & 0xFFFFFF00) | low;

                    high &= 0x07;

                    if (high != 0)
                    {
                        count = high + 1;
                    }
                    else
                    {
                        try
                        {
                            count = file.read();
                        }
                        catch (IOException e)
                        {
                            done = true;
                            continue;
                        }

                        if (count == 0)
                        {
                            done = true;
                        }
                        if (count <= 1)
                        {
                            continue;
                        }
                    }
                }
                else
                {
                    count = (get_bit() << 1) | get_bit();
                    count++;

                    try
                    {
                        offset = 0xFFFFFF00 | file.read();
                    }
                    catch (IOException e)
                    {
                        done = true;
                        continue;
                    }
                }
            }

            if ((write_pos + offset) < 0)
            {
                System.out.println("Argh, weird offset!");

                try
                {
                    file.close();
                }
                catch (IOException e)
                {
                }

                return;
            }

            while (count >= 0)
            {
                int buf = data_buffer[write_pos + offset];

                data_buffer[write_pos++] = buf;
                count--;
            }
        }

        try
        {
            read_size = file.getFilePointer() - start_pos;
            file.close();
        }
        catch (IOException e)
        {
        }
    }

    public int read()
    {
        if (read_pos >= write_pos)
        {
            return -1;
        }

        return data_buffer[read_pos++];
    }

    public int[] readAll()
    {
        if (read_pos >= write_pos)
        {
            return null;
        }

        int buf[] = new int[write_pos];
        int i;

        for (i = 0; i < write_pos; i++)
        {
            buf[i] = data_buffer[i];
        }

        read_pos = write_pos;

        return buf;
    }

    public long readCount()
    {
        return read_size;
    }
}
