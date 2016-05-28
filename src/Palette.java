import java.awt.image.IndexColorModel;

public class Palette
{
    private int colours[];

    public Palette(int c[])
    {
        colours = new int[c.length];

        for (int i = 0; i < c.length; i++)
        {
            colours[i] = c[i];
        }
    }

    public IndexColorModel getPalette()
    {
        int c, r, g, b;

        byte R[] = new byte[colours.length];
        byte G[] = new byte[colours.length];
        byte B[] = new byte[colours.length];

        for (int i = 0; i < colours.length; i++)
        {
            c = colours[i];

            r = c & 0x0F;
            g = (c & 0xF0) >> 4;
            b = (c & 0x0F00) >> 8;

            R[i] = (byte) Math.floor((r / 15.0) * 255);
            G[i] = (byte) Math.floor((g / 15.0) * 255);
            B[i] = (byte) Math.floor((b / 15.0) * 255);
        }

        return new IndexColorModel(6, colours.length, R, G, B);
    }
}
