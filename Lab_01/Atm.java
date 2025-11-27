import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Atm 
{
    //
    static DatabaseManager db = new DatabaseManager();
    
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

    // 
    static Journal opJournal = new Journal(opsAllFile, opsSessionFile, db);
    static Journal cashJournal = new Journal(cashAllFile, cashSessionFile, db);
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) 
    {
        System.out.println("=== Банкомат ===");
        
        try 
        {
            loadUsersAndAuthorize();
            initCashHoldersFromDb();
        } catch (InvalidCredentialsException e) 
        {
            System.out.println("Ошибка авторизации: " + e.getMessage());
            db.close();
            System.exit(0);
        }

        int choice;
        do 
        {
            printMenu();
            choice = readInt("Выберите пункт меню : ");
            
            try 
            {
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
                        if (isAdmin()) {
                            printCashReport();
                        }
                        else System.out.println("Нет прав доступа.");
                        break;
                    case 6:
                        // 
                        db.printOperations();
                        break;
                    case 0:
                        System.out.println("Выход...");
                        Operation op = new Operation("Выход", "-", currentUserName, opJournal);
                        op.execute("OK");
                        break;
                    default:
                        throw new InvalidOperationException("Неверный пункт меню. Выберите 0-6.");
                }
            } catch (AtmException e) 
            {
                System.out.println("Ошибка операции: " + e.getMessage());
            }
        } while (choice != 0);

        System.out.println("Сеанс завершён.");
        // 
        db.close();
    }

    // 
    static void loadUsersAndAuthorize() throws InvalidCredentialsException
    {
        System.out.println("Авторизация по карте");
        String cardInput = readLine("Введите номер карты: ");
        String pinInput = readLine("Введите PIN: ");
        
        // 
        User user = db.authenticate(cardInput, pinInput);
        
        currentUserName = user.getName();
        currentUserRole = user.getRole();
        userBalance = user.getBalance();

        System.out.println("Успешный вход. Пользователь: " + currentUserName + " (" + currentUserRole + ")");
        Operation op = new Operation("Авторизация", "card=" + cardInput, currentUserName, opJournal);
        op.execute("OK");
    }

    static void printMenu() 
    {
        System.out.println("\nМеню");
        if (isAdmin()) 
        {
            System.out.println("Баланс банкомата: " + getAtmBalance() + " руб.");
            System.out.println("4. Пополнение банкомата (сервис)");
            System.out.println("5. Отчет по купюрам (сервис)");
            System.out.println("6. История операций из БД");
            System.out.println("0. Выход");
        }
        else
        {
            System.out.println("Наличность банкомата: " + getAtmBalance() + " руб.");
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

    static void withdrawCash() throws AtmException
    {
        System.out.println("\n--- Снятие наличных ---");
        int amount = readInt("Введите сумму для снятия: ");
        
        if (amount <= 0) 
        {
            throw new InvalidOperationParametersException("Сумма должна быть больше нуля");
        }
        
        if (amount > userBalance) 
        {
            Operation op = new Operation("Снятие", "amount=" + amount, currentUserName, opJournal);
            op.execute("FAIL (balance)");
            throw new InsufficientCashException("Недостаточно средств на счете");
        }

        int[] take = new int[cashHolders.length];
        try {
            boolean ok = calcWithdraw(amount, take);
            if (!ok) 
            {
                Operation op = new Operation("Снятие", "amount=" + amount, currentUserName, opJournal);
                op.execute("FAIL (cash)");
                throw new InsufficientCashException("Недостаточно наличных в банкомате");
            }
        } catch (InsufficientDenominationException e) 
        {
            Operation op = new Operation("Снятие", "amount=" + amount, currentUserName, opJournal);
            op.execute("FAIL (denomination)");
            throw new InsufficientCashException("Недостаточное количество купюр");
        }

        for (int i = 0; i < cashHolders.length; i++) 
        {
            int cnt = take[i];
            if (cnt > 0) 
            {
                try 
                {
                    cashHolders[i].removeCash(cnt);
                } 
                catch (InsufficientDenominationException e) 
                {
                    throw new InsufficientDenominationException(e.getMessage());
                }
                cashJournal.log("Снятие", String.valueOf(cashHolders[i].getDenomination()), String.valueOf(-cnt));
            }
        }
        userBalance -= amount;
        
        // 
        db.updateBalance(currentUserName, userBalance);

        System.out.println("Выдано " + amount + " руб. Купюры:");
        for (int i = 0; i < cashHolders.length; i++) 
        {
            if (take[i] > 0) 
            {
                System.out.println("  " + cashHolders[i].getDenomination() + " x " + take[i]);
            }
        }
        Operation op = new Operation("Снятие", "amount=" + amount, currentUserName, opJournal);
        op.execute("OK");
    }

    static boolean calcWithdraw(int amount, int[] resultTake) throws InsufficientDenominationException
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

    static void depositCash() throws AtmException
    {
        System.out.println("\nВнесение наличных");
        int total = 0;
        for (int i = 0; i < cashHolders.length; i++) 
        {
            int cnt = readInt("Сколько купюр номиналом " + cashHolders[i].getDenomination() + " внести? ");
            
            if (cnt < 0) 
            {
                throw new InvalidOperationParametersException("Количество не может быть отрицательным");
            }
            if (cnt > 0) 
            {
                try 
                {
                    cashHolders[i].addCash(cnt);
                } catch (InvalidDenominationException e)
                {
                    throw new InvalidOperationParametersException(e.getMessage());
                }
                total += cnt * cashHolders[i].getDenomination();
                cashJournal.log("Внесение(польз.)", String.valueOf(cashHolders[i].getDenomination()), String.valueOf(cnt));
            }
        }
        userBalance += total;
        
        // 
        db.updateBalance(currentUserName, userBalance);
        System.out.println("Внесено всего: " + total + " руб.");
        Operation op = new Operation("Внесение", "total=" + total, currentUserName, opJournal);
        op.execute("OK");
    }

    static void payService() throws AtmException
    {
        System.out.println("\nОплата услуг");
        String service = readLine("Введите название услуги: ");
        int amount = readInt("Введите сумму оплаты: ");
        
        if (amount <= 0) {
            throw new InvalidOperationParametersException("Сумма должна быть больше нуля");
        }
        
        if (amount > userBalance) 
        {
            Operation op = new Operation("Оплата услуг", "service=" + service, currentUserName, opJournal);
            op.execute("FAIL (balance)");
            throw new InsufficientCashException("Недостаточно средств");
        }
        userBalance -= amount;
        
        // 
        db.updateBalance(currentUserName, userBalance);
        
        System.out.println("Услуга \"" + service + "\" оплачена на " + amount + " руб.");
        Operation op = new Operation("Оплата услуг", "service=" + service, currentUserName, opJournal);
        op.execute("OK");
    }

    static void refillAtm() throws AtmException
    {
        System.out.println("\n--- Пополнение банкомата (сервис) ---");
        for (int i = 0; i < cashHolders.length; i++) 
        {
            int cnt = readInt("Сколько купюр номиналом " + cashHolders[i].getDenomination() + " добавить? ");
            
            if (cnt < 0) {
                throw new InvalidOperationParametersException("Количество не может быть отрицательным");
            }
            
            if (cnt > 0) {
                try {
                    cashHolders[i].addCash(cnt);
                    // 
                    try {
                        db.updateCashHolder(cashHolders[i].getDenomination(), cashHolders[i].getCount());
                        db.logCashChange("Пополнение(сервис)", cashHolders[i].getDenomination(), cnt);
                        System.out.println("[DEBUG] denom=" + cashHolders[i].getDenomination() + " -> " + cashHolders[i].getCount());
                    } 
                    catch (Exception e) 
                    {                    
                    //
                        System.out.println("Ошибка обновления cash_holders в БД: " + e.getMessage());
                    }
                } 
                catch (InvalidDenominationException e) 
                {
                    throw new InvalidOperationParametersException(e.getMessage());
                }
                cashJournal.log("Пополнение(сервис)", String.valueOf(cashHolders[i].getDenomination()), String.valueOf(cnt));
            }
        }
        Operation op = new Operation("Пополнение банкомата", "-", currentUserName, opJournal);
        op.execute("OK");
    }

    static void printCashReport() throws AtmException
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
        } catch (IOException e) 
        {
            System.out.println("Ошибка чтения отчета: " + e.getMessage());
        }

        System.out.println(line);
        Operation op = new Operation("Отчет по купюрам", "-", currentUserName, opJournal);
        op.execute("OK");
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
            } catch (NumberFormatException e) {
                System.out.println("Введите корректное целое число.");
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

    // 
    static void initCashHoldersFromDb()
    {
        CashHolder[] dbHolders = db.getAllCashHolders();
        if (dbHolders != null && dbHolders.length > 0) 
        {
            cashHolders = dbHolders;
        }
        else 
        {
            for (CashHolder holder : cashHolders) 
            {
                db.updateCashHolder(holder.getDenomination(), holder.getCount());
            }
        }
    }

    // 
    static int getAtmBalance()
    {
        int total = 0;
        for (CashHolder holder : cashHolders) 
        {
            total += holder.getCount() * holder.getDenomination();
        }
        return total;
    }
}
