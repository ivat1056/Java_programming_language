import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Atm 
{
    
    // static int[] denoms = {5000, 2000, 1000, 500, 200, 100, 50, 10, 5};
    // static int[] counts = {10, 10, 10, 10, 10, 10, 10, 10, 10}; 

    static CashHolder[] cashHolders = 
    {
        new CashHolder(5000, 10),
        new CashHolder(2000, 10),
        new CashHolder(1000, 10),
        new CashHolder(500, 10),
        new CashHolder(200, 10),
        new CashHolder(100, 10),
        new CashHolder(50, 10),
        new CashHolder(10, 10),
        new CashHolder(5, 10)
    };

    static int userBalance = 0;
    static String currentUserName = "unknown";
    static String currentUserRole = "user"; 
    static String sessionId = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    static String opsSessionFile = "ops_" + sessionId + ".txt";
    static String cashSessionFile = "cash_" + sessionId + ".txt";
    static String opsAllFile = "ops_all.txt";
    static String cashAllFile = "cash_all.txt";

    
    static Journal opJournal = new Journal(opsAllFile, opsSessionFile);
    static Journal cashJournal = new Journal(cashAllFile, cashSessionFile);

    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) 
    {
        System.out.println("=== Банкомат ===");
        loadUsersAndAuthorize();
        int choice;
        do {
            printMenu();
            choice = readInt("Выберите пункт меню : ");

            switch (choice) 
            {
                case 1:
                    withdrawCash();  
                    break;
                case 2:
                    depositCash();
                    break;
                case 3:
                    payService();
                    break;
                case 4:
                    if (isAdmin()) refillAtm();
                    else System.out.println("Нет прав доступа.");
                    break;
                case 5:
                    if (isAdmin()) printCashReport();
                    else System.out.println("Нет прав доступа.");
                    break;
                case 0:
                    System.out.println("Выход...");
                    //
                    Operation op = new Operation("Выход", "-", currentUserName, opJournal);
                    op.execute("OK");
                    //
                    break;
                default:
                    System.out.println("Неверный пункт меню.");
            }
        } while (choice != 0);

        System.out.println("Сеанс завершён.");
    }

    static void loadUsersAndAuthorize() 
    {
        System.out.println("Авторизация по карте");
        String cardInput = readLine("Введите номер карты: ");
        String pinInput = readLine("Введите PIN: ");
        boolean found = false;

        File f = new File("users.txt");
        if (f.exists()) 
        {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) 
            {
                String line;
                while ((line = br.readLine()) != null) 
                {
                    String[] parts = line.trim().split(";");
                    if (parts.length < 5) continue;
                    String card = parts[0];
                    String pin = parts[1];
                    String name = parts[2];
                    String role = parts[3];
                    int balance = Integer.parseInt(parts[4]);

                    if (card.equals(cardInput) && pin.equals(pinInput)) 
                    {
                        currentUserName = name;
                        currentUserRole = role;
                        userBalance = balance;
                        found = true;
                        break;
                    }
                }
            } catch (IOException e) 
            {
                System.out.println("Ошибка чтения users.txt: " + e.getMessage());
            }
        } 

        if (!found) 
        {
            System.out.println("Авторизация не удалась. Завершение работы.");
            //
            Operation op = new Operation("Авторизация", "card=" + cardInput, currentUserName, opJournal);
            op.execute("FAIL");
            System.exit(0);
            //
        } 
        else 
        {
            System.out.println("Успешный вход. Пользователь: " + currentUserName + " (" + currentUserRole + ")");
            // 
            Operation op = new Operation("Авторизация", "card=" + cardInput, currentUserName, opJournal);
            op.execute("OK");
            //
        }
    }

    static void printMenu() 
    {
        System.out.println("\nМеню");
        System.out.println("Текущий баланс: " + userBalance + " руб.");
        if (isAdmin()) 
        {
            System.out.println("4. Пополнение банкомата (сервис)");
            System.out.println("5. Отчет по купюрам (сервис)");
            System.out.println("0. Выход");
        }
        else
        {
            System.out.println("1. Снятие наличных");
            System.out.println("2. Внесение наличных");
            System.out.println("3. Оплата услуг");
            System.out.println("0. Выход");
        }
    }

    static boolean isAdmin() 
    {
        return "admin".equalsIgnoreCase(currentUserRole);
    }

    static void withdrawCash() 
    {
        System.out.println("\n--- Снятие наличных ---");
        int amount = readInt("Введите сумму для снятия: ");
        if (amount <= 0 || amount > userBalance) 
        {
            System.out.println("Некорректная сумма или недостаточно средств на счете.");
            // 
            Operation op = new Operation("Снятие", "amount=" + amount, currentUserName, opJournal);
            op.execute("FAIL (balance)");
            //
            return;
        }

        int[] take = new int[cashHolders.length];
        boolean ok = calcWithdraw(amount, take);
        if (!ok) 
        {
            System.out.println("Невозможно выдать сумму.");
            // 
            Operation op = new Operation("Снятие", "amount=" + amount, currentUserName, opJournal);
            op.execute("FAIL (cash)");
            //
            return;
        }

        for (int i = 0; i < cashHolders.length; i++) 
        {
            int cnt = take[i];
            cashHolders[i].removeCash(cnt);
            if (cnt != 0) 
            {
                cashJournal.log("Снятие", String.valueOf(cashHolders[i].getDenomination()), String.valueOf(-cnt));
            }
        }
        userBalance -= amount;

        System.out.println("Выдано " + amount + " руб. Купюры:");
        for (int i = 0; i < cashHolders.length; i++) 
        {
            if (take[i] > 0) 
            {
                System.out.println("  " + cashHolders[i].getDenomination() + " x " + take[i]);
            }
        }
        // 
        Operation op = new Operation("Снятие", "amount=" + amount, currentUserName, opJournal);
        op.execute("OK");
        //
    }

    static boolean calcWithdraw(int amount, int[] resultTake) 
    {
        int remaining = amount;
        for (int i = 0; i < cashHolders.length; i++) 
        {
            int denom = cashHolders[i].getDenomination();
            int maxNeed = remaining / denom;  
            int take = Math.min(maxNeed, cashHolders[i].getCount());
            resultTake[i] = take;
            remaining -= take * denom;
        }
        return remaining == 0;
    }

    static void depositCash() 
    {
        System.out.println("\nВнесение наличных");
        int total = 0;
        for (int i = 0; i < cashHolders.length; i++) 
        {
            int cnt = readInt("Сколько купюр номиналом " + cashHolders[i].getDenomination() + " внести? ");
            if (cnt < 0) cnt = 0;
            cashHolders[i].addCash(cnt);
            total += cnt * cashHolders[i].getDenomination();
            if (cnt != 0) 
            {
                cashJournal.log("Внесение(польз.)", String.valueOf(cashHolders[i].getDenomination()), String.valueOf(cnt));
            }
        }
        userBalance += total;
        System.out.println("Внесено всего: " + total + " руб.");
        // 
        Operation op = new Operation("Внесение", "total=" + total, currentUserName, opJournal);
        op.execute("OK");
        //
    }

    static void payService() 
    {
        System.out.println("\nОплата услуг");
        String service = readLine("Введите название услуги (например, Интернет): ");
        int amount = readInt("Введите сумму оплаты: ");
        
        if (amount <= 0 || amount > userBalance) 
        {
            System.out.println("Некорректная сумма или недостаточно средств на счете.");
            // 
            Operation op = new Operation("Оплата услуг", "service=" + service + ";amount=" + amount, currentUserName, opJournal);
            op.execute("FAIL (balance)");
            //
            return;
        }
        userBalance -= amount;
        System.out.println("Услуга \"" + service + "\" оплачена на " + amount + " руб.");
        // 
        Operation op = new Operation("Оплата услуг", "service=" + service + ";amount=" + amount, currentUserName, opJournal);
        op.execute("OK");
        //
    }

    static void refillAtm() 
    {
        System.out.println("\n--- Пополнение банкомата (сервис) ---");
        for (int i = 0; i < cashHolders.length; i++) 
        {
            int cnt = readInt("Сколько купюр номиналом " + cashHolders[i].getDenomination() + " добавить? ");
            if (cnt < 0) cnt = 0;
            cashHolders[i].addCash(cnt);
            if (cnt != 0) 
            {
                cashJournal.log("Пополнение(сервис)", String.valueOf(cashHolders[i].getDenomination()), String.valueOf(cnt));
            }
        }
        // 
        Operation op = new Operation("Пополнение банкомата", "-", currentUserName, opJournal);
        op.execute("OK");
        //
    }

    static void printCashReport() 
    {
        System.out.println("\n--- Отчет по операциям с купюрами (сеанс) ---");
        File f = new File(cashSessionFile);
        if (!f.exists()) 
        {
            System.out.println("Нет данных за текущий сеанс.");
            return;
        }

        String line = "+-----------------+---------------------+---------+---------+";
        System.out.println(line);
        System.out.println("| Дата/время      | Операция            | Номинал | Кол-во  |");
        System.out.println(line);

        try (BufferedReader br = new BufferedReader(new FileReader(f))) 
        {
            String row;
            while ((row = br.readLine()) != null) 
            {
                String[] parts = row.split(";");
                if (parts.length < 4) continue;
                String dt = padRight(parts[0], 15);
                String op = padRight(parts[1], 19);
                String denom = padLeft(parts[2], 7);
                String cnt = padLeft(parts[3], 7);
                System.out.println("| " + dt + " | " + op + " | " + denom + " | " + cnt + " |");
            }
        } 
        catch (IOException e) 
        {
            System.out.println("Ошибка чтения отчета: " + e.getMessage());
        }

        System.out.println(line);
        // 
        Operation op = new Operation("Отчет по купюрам", "file=" + cashSessionFile, currentUserName, opJournal);
        op.execute("OK");
        //
    }

    static String now() 
    {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    static String readLine(String msg) 
    {
        System.out.print(msg);
        return scanner.nextLine();
    }

    static int readInt(String msg) 
    {
        while (true) 
        {
            try 
            {
                System.out.print(msg);
                String s = scanner.nextLine();
                return Integer.parseInt(s.trim());
            } 
            catch (NumberFormatException e) 
            {
                System.out.println("Введите целое число.");
            }
        }
    }

    static String padRight(String s, int n) 
    {
        if (s.length() > n) return s.substring(0, n);
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) sb.append(' ');
        return sb.toString();
    }

    static String padLeft(String s, int n) 
    {
        if (s.length() > n) return s.substring(0, n);
        StringBuilder sb = new StringBuilder();
        while (sb.length() + s.length() < n) sb.append(' ');
        sb.append(s);
        return sb.toString();
    }

    static String padLeft(int value, int n) 
    {
        return padLeft(String.valueOf(value), n);
    }
}