import java.util.Scanner;
public class task_01 
{
    public static void main(String[] args)
    {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int m = sc.nextInt();
        int [][] array = new int[n][m];
        for (int i = 0; i<n; i++)
        {
            for (int j = 0; j<m; j++)
            {
                array[i][j] = sc.nextInt();
            }
        }
        int max = array[0][0];
        for (int i = 0; i<n; i++)
        {
            for (int j = 0; j<m; j++)
            {
                if (array[i][j]>max)
                {
                    max = array[i][j];
                }
            }
        }
        System.out.println(max);
    }
}    