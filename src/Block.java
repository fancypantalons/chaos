import java.awt.image.*;

public class Block
{
  Chunk chunks[];
  int chunk_seq[];
  BufferedImage myImage;
  IndexColorModel pal;

  public Block(Chunk c[], Palette p, int cs[])
  {
    chunks = c;
    chunk_seq = cs;
    myImage = null;
    pal = p.getPalette();
  }

  void renderImage()
  {
    int x, y;
    int px, py;

    int chunk_data;
    int chunk_num;
    boolean x_flip;
    boolean y_flip;

    int[] line = new int[16];
    int pixel;

    myImage = new BufferedImage(128, 128, 
                                BufferedImage.TYPE_BYTE_INDEXED,
                                pal);

    WritableRaster raster = myImage.getRaster();

    for (y = 0, py = 0; y < 8; y++, py += 16)
      {
	for (x = 0, px = 0; x < 8; x++, px += 16)
	  {
	    chunk_data = chunk_seq[(y * 8) + x];

	    chunk_num = chunk_data & 0x03FF;
	    x_flip = (chunk_data & 0x0400) != 0;
	    y_flip = (chunk_data & 0x0800) != 0;

	    if (chunk_num < chunks.length)
	      {
                Raster chunkRaster = chunks[chunk_num].getImage().getRaster();

                for (int j = 0; j < 16; j++)
                  {
                    int yoffs = (y_flip) ? 15 - j : j;
                    
                    chunkRaster.getPixels(0, j, 16, 1, line);
                    
                    if (x_flip)
                      {
                        for (int i = 0; i < 8; i++)
                          {
                            pixel = line[i];
                            line[i] = line[15 - i];
                            line[15 - i] = pixel;
                          }
                      }
                    
                    raster.setPixels(px, py + yoffs, 16, 1, line);
                  }
              }
	  }
      }
  }

  public BufferedImage getImage()
  {
    if (myImage == null) renderImage();

    return myImage;
  }
}

