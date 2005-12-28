import java.io.*;
import java.lang.*;

public class SMDFile extends RandomAccessFile
{
  long pos;

  public SMDFile(String name, String mode) throws IOException
  {
    super(name, mode);

    pos = 0;
  }

  public int read() throws IOException
  {
    seekToPos(pos++);

    return super.read();
  }

  public void seek(long pos) throws IOException
  {
    this.pos = pos;
  }

  public void skip(long offs)
  {
    this.pos += offs;
  }

  public int[] readAll() throws IOException
  {
    int buf[] = new int[(int)(super.length()) - 512];
    int i;
    
    for (i = 0; i < buf.length; i++)
      {
        seekToPos(i);
        
        buf[i] = super.read();
      }

    return buf;
  }

  public void write(int c) throws IOException
  {
    seekToPos(pos++);
    super.write(c);
  }

  public long getFilePointer()
  {
    return pos;
  }

  public long length() throws IOException
  {
    return super.length() - 512;
  }

  void seekToPos(long position) throws IOException
  {
    long block_num = position >> 14;
    long block_offs = position % 16384;
    long offset = 512 + (block_num << 14);

    if ((block_offs & 1) == 1)
      offset += (block_offs - 1) >> 1;
    else
      offset += (block_offs >> 1) + 8192;

    super.seek(offset);
  }
}
