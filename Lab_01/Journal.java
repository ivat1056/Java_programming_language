import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Journal 
{
    private String allFile;
    private String sessionFile;

    public Journal(String allFile, String sessionFile) 
    {
        this.allFile = allFile;
        this.sessionFile = sessionFile;
    }

    public void log(String name, String details, String result, String user) 
    {
        String dt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String line = dt + ";" + user + ";" + name + ";" + details + ";" + result;
        writeLine(sessionFile, line);
        writeLine(allFile, line);
    }

    public void log(String operation, String denom, String count) 
    {
        String dt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String line = dt + ";" + operation + ";" + denom + ";" + count;
        writeLine(sessionFile, line);
        writeLine(allFile, line);
    }

    private void writeLine(String fileName, String line) 
    {
        try (FileWriter fw = new FileWriter(fileName, true)) 
        {
            fw.write(line + System.lineSeparator());
        } 
        catch (IOException e) 
        {
            System.out.println("Ошибка записи в файл " + fileName + ": " + e.getMessage());
        }
    }
}