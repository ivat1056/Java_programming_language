import java.util.Scanner;

interface Printer 
{
    void print();
}

class ConsolePrinter implements Printer 
{
    public void print() 
    {
        System.out.println("Hello, console!");
    }
}



public class task_02
{
	public static void main (String[] args  )
	{
        // int rand = 20 + (int) (Math.random() * (100-20 +1));
        // System.out.println(rand);
        // Scanner sc = new Scanner(System.in);
        // String s = sc.nextLine();
        // System.out.println(s.length());

        Printer p = new ConsolePrinter();
        p.print();


	}


}
