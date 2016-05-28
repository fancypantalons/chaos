import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;

public class Tile
{
    private int tile[];

    public Tile(int bytes[])
    {
        int x, y;

        tile = new int[64];

        for (y = 0; y < 8; y++)
            for (x = 0; x < 8; x++)
            {
                tile[(y * 8) + x] = (bytes[y] >> (x * 4)) & 0x0F;
            }
    }

    public int[] getPixels(int pal_idx, boolean x_flip, boolean y_flip)
    {
        int x, y, x_loc, y_loc;
        int[] pixels = new int[64];

        for (y = 0; y < 8; y++)
        {
            for (x = 0; x < 8; x++)
            {
                x_loc = (x_flip) ? x : 7 - x;
                y_loc = (y_flip) ? 7 - y : y;

                pixels[(y_loc * 8) + x_loc] = tile[(y * 8) + x] + (pal_idx * 16);
            }
        }

        return pixels;
    }

    public BufferedImage getImage(IndexColorModel pal, int pal_idx,
                                  boolean x_flip, boolean y_flip)
    {
        BufferedImage img = new BufferedImage(
            8, 8,
            BufferedImage.TYPE_BYTE_INDEXED,
            pal
        );
        WritableRaster raster = img.getRaster();

        raster.setPixels(0, 0, 8, 8, getPixels(pal_idx, x_flip, y_flip));

        return img;
    }
}
