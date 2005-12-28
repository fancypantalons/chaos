import java.awt.image.*;

class Chunk
{
  Tile tiles[];
  IndexColorModel pal;
  int tile_seq[];

  BufferedImage myImage;

  public Chunk(Tile t[], Palette p, int seq[])
  {
    tiles = t;
    pal = p.getPalette();
    tile_seq = seq;
    myImage = null;
  }

  void renderImage()
  {
    int x, y;
    int px, py;

    myImage = new BufferedImage(16, 16, 
                                BufferedImage.TYPE_BYTE_INDEXED, 
                                pal);

    WritableRaster raster = myImage.getRaster();

    for (y = 0, py = 0; y < 2; y++, py += 8)
      {
	for (x = 0, px = 0; x < 2; x++, px += 8)
	  {
	    int t_data;
	    int t_num, p_num;
	    boolean x_flip, y_flip;

	    t_data = tile_seq[(y * 2) + x];

	    t_num = t_data & 0x07FF;
	    x_flip = (t_data & 0x0800) != 0;
	    y_flip = (t_data & 0x1000) != 0;
	    p_num = (t_data & 0x6000) >> 13;

	    if (t_num < tiles.length)
	      {
                raster.setPixels(px, py, 8, 8, 
                                 tiles[t_num].getPixels(p_num, x_flip, y_flip));
	      }
	    else
	      System.out.println("Invalid tile at number " + t_num +
				 ", data " + Integer.toString(t_data, 16));
	  }
      }
  }

  public BufferedImage getImage()
  {
    if (myImage == null) renderImage();

    return myImage;
  }
}
