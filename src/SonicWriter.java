public class SonicWriter
{
    int buffer[];
    int output_buf[];

    int field;
    int bit_count;
    int field_pos;
    int pos;
    int output_pos;
    int buf_size;

    public SonicWriter(int d[])
    {
        buffer = d;
        output_buf = new int[65535];

        field = 0;
        bit_count = 0;
        field_pos = 0;
        pos = 0;
        output_pos = 2;
        buf_size = d.length;

        compress();
    }

    public int[] readAll()
    {
        return output_buf;
    }

    public int compressedSize()
    {
        return output_pos;
    }

    void add_bit(int bit)
    {
        bit_count++;
        field |= bit << (bit_count - 1);

        if (bit_count == 16)
        {
            output_buf[field_pos] = field & 0xFF;
            output_buf[field_pos + 1] = (field >> 8) & 0xFF;

            field_pos = output_pos;
            output_pos += 2;

            field = 0;
            bit_count = 0;
        }
    }

    int seg_cmp(int start, int length, int offset)
    {
        int i;

        for (i = 0; i < length; i++)
            if (buffer[start + i] != buffer[start + i + offset])
            {
                return -1;
            }

        return 0;
    }

    int find_segment(int pos, int length)
    {
        int cur_pos = pos - length;

        while (true)
        {
            if (cur_pos < 0)
            {
                return 0;
            }

            if (seg_cmp(pos, length, (cur_pos - pos)) == 0)
            {
                return cur_pos - pos;
            }
            else
            {
                cur_pos--;
            }

            if (cur_pos < 0)
            {
                return 0;
            }
            if ((pos - cur_pos) > 4096)
            {
                return 0;
            }
        }
    }

    void compress()
    {
        int length = 0, offset = 0;
        int hi = 0, lo = 0;

        int tmp_offset = 0;

        while (pos < buf_size)
        {
            length = 1;
            offset = 0;

	/* First, try to capture any repeating bytes. */

            while ((pos + length) < buf_size)
            {
                if (length >= 256)
                {
                    break;
                }
                if (buffer[pos] != buffer[pos + length])
                {
                    break;
                }
                length++;
            }

	/* Next, see if there's somewhere we can copy the next sequence from. */

            while ((tmp_offset = find_segment(pos, length)) != 0)
            {
                offset = tmp_offset;
                length++;

                if ((pos + length) > buf_size)
                {
                    break;
                }
                if (length > 256)
                {
                    break;
                }
            }

	/* Now, if there is a copy location, check if the sequence is adjacent to
       this one.  If so, check if it can be used for a longer sequence run. */

            if (offset != 0)
            {
                length--;

                if ((length + offset) == 0)
                {
                    int seg_length = length;

                    while ((pos + length) < buf_size)
                    {
                        if (buffer[(pos + length) - seg_length] != buffer[pos + length])
                        {
                            break;
                        }

                        if ((pos + length + 1) > buf_size)
                        {
                            break;
                        }
                        if ((length + 1) > 256)
                        {
                            break;
                        }

                        length++;
                    }
                }
            }
            else if ((pos > 0) && (buffer[pos - 1] == buffer[pos]))
            {
                offset = -1;  /* No copy loc... is the previous byte the same as this?
			   If so, use it to do the replication. */
            }

            if ((offset == 0) || ((length <= 2) && (offset < -127)) ||
                (length < 2)) /* Spit out a byte if there was no sizeable run */
            {
                add_bit(1);

                if (length == 2)
                {
                    length = 1;
                }

                output_buf[output_pos++] = buffer[pos++];
                offset = -1;

                if ((buffer[pos] != buffer[pos + length - 1]) || (length == 1))
                {
                    length--;
                }
            }

            if (length == 0)
            {
                continue;
            }

	/* Well, it seems we have a run.  Time to encode it. */

            add_bit(0);

            if ((length <= 5) && (offset >= -127))
            {
                add_bit(0);
                add_bit(((length - 2) >> 1) & 1);
                add_bit((length - 2) & 1);

                output_buf[output_pos++] = offset;
            }
            else
            {
                add_bit(1);

                if (length <= 9)
                {
                    hi = (offset >> 5) & 0xF8;
                    hi |= (length - 2) & 0x07;
                }
                else
                {
                    hi = (offset >> 5) & 0xF8;
                }

                lo = offset & 0x00FF;

                output_buf[output_pos++] = lo;
                output_buf[output_pos++] = hi;

                if (length > 9)
                {
                    output_buf[output_pos++] = length - 1;
                }
            }

	/* Now, adjust our position and continue. */

            pos += length;
        }

        add_bit(0);
        add_bit(1);

        hi = (offset >> 5) & 0xF8;
        lo = offset & 0x00FF;

        output_buf[output_pos++] = lo;
        output_buf[output_pos++] = hi;
        output_buf[output_pos++] = 0;

        output_buf[field_pos] = field & 0xFF;
        output_buf[field_pos + 1] = (field >> 8) & 0xFF;
    }
}
