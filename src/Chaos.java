public class Chaos
{
    public static void main(String params[]) throws Exception
    {
        MainWin win;

        if (params.length > 0)
        {
            win = new MainWin(params[0]);
        }
        else
        {
            win = new MainWin((String) null);
        }


        win.show();
    }
}

