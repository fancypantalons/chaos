import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

public class Map
{
    private static final int width = 128;
    private static final int height = 16;

    Block blocks[];
    int map_seq[];

    IndexColorModel pal;

    public Map(Block b[], Palette p, int seq[])
    {
        blocks = b;
        map_seq = seq;

        pal = p.getPalette();
    }

    public int[] getMap()
    {
        return map_seq;
    }

    private WritableRaster getVirtualImage(int xs, int ys, int w, int h)
    {
        int x, y;

        int x_start = xs / 128;
        int x_end = (xs + w) / 128 + 1;

        int y_start = ys / 128;
        int y_end = (ys + h) / 128 + 1;

        int new_w = (x_end - x_start) * 128;
        int new_h = (y_end - y_start) * 128;

        int t_x, t_y;

        BufferedImage image = new BufferedImage(
            new_w, new_h,
            BufferedImage.TYPE_BYTE_INDEXED,
            pal
        );
        WritableRaster raster = image.getRaster();

        for (y = y_start, t_y = 0; y < y_end; y++, t_y += 128)
        {
            for (x = x_start, t_x = 0; x < x_end; x++, t_x += 128)
            {
                if (((y * 2) * width) + x < map_seq.length)
                {
                    int num = ((y * 2) * width) + x;

                    int fore_num = map_seq[num];
                    int back_num = map_seq[num + width];

                    BufferedImage fore = blocks[fore_num].getImage();
                    BufferedImage back = blocks[back_num].getImage();

                    raster.setRect(t_x, t_y, fore.getRaster());
                }
            }
        }

        return raster;
    }

    public BufferedImage getImage(int xs, int ys, int w, int h)
    {
        BufferedImage image = new BufferedImage(
            w, h,
            BufferedImage.TYPE_BYTE_INDEXED,
            pal
        );

        WritableRaster raster = image.getRaster();
        Raster v_raster = getVirtualImage(xs, ys, w, h);

        int[] buffer = new int[w * h];

        v_raster.getPixels(xs & 0x7F, ys & 0x7F, w, h, buffer);
        raster.setPixels(0, 0, w, h, buffer);

        return image;
    }
}
