import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class Main {

    /* ===================== Конфигурация / пути ===================== */
    static final int[] DENOMS = {5000, 2000, 1000, 500, 200, 100, 50}; // по убыванию
    static final String FILE_ACCESS   = "access.txt";
    static final String FILE_ACCOUNTS = "accounts.txt";
    static final String FILE_CASHBOX  = "cashbox.txt";

    static final String DIR_LOGS      = "logs";
    static String SESSION_TAG = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
    static final String OPS_SESSION   = () -> Paths.get(DIR_LOGS, "ops_"  + SESSION_TAG + ".log").toString();
    static final String CASH_SESSION  = () -> Paths.get(DIR_LOGS, "cash_" + SESSION_TAG + ".log").toString();
    static final String OPS_ALL       = Paths.get(DIR_LOGS, "ops_all.log").toString();
    static final String CASH_ALL      = Paths.get(DIR_LOGS, "cash_all.log").toString();

    /* ===================== Хранилища в памяти (без классов) ===================== */
    // accounts: accountId -> balanceKop (копейки)
    static final Map<String, Long> ACCOUNTS = new HashMap<>();
    // cards: cardNumber -> {pin, accountId, role(USER|SERVICE), ownerName}
    static final Map<String, String[]> CARDS = new HashMap<>();
    // cash cassette: denomRub -> count
    static final Map<Integer, Integer> CASH = new TreeMap<>(Collections.reverseOrder());

    /* ===================== Утилиты ===================== */
    static String nowTs() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
    }
    static void ensureFilesExist() throws IOException {
        // Папка логов
        Files.createDirectories(Paths.get(DIR_LOGS));

        // Дефолтные файлы конфигурации, если их нет
        if (!Files.exists(Paths.get(FILE_ACCESS))) {
            Files.writeString(Paths.get(FILE_ACCESS),
                    String.join("\n",
                            "# card;pin;accountId;role;owner",
                            "1111222233334444;1234;A1;USER;Иван Матвеев",
                            "5555666677778888;4321;A2;USER;Анна Петрова",
                            "0000000000000000;9999;-;SERVICE;Инженер"
                    ), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        if (!Files.exists(Paths.get(FILE_ACCOUNTS))) {
            Files.writeString(Paths.get(FILE_ACCOUNTS),
                    String.join("\n",
                            "# accountId;balanceRub",
                            "A1;25000",
                            "A2;7500"
                    ), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        if (!Files.exists(Paths.get(FILE_CASHBOX))) {
            // начальная кассета
            StringBuilder sb = new StringBuilder("# denom=count\n");
            sb.append("5000=10\n2000=20\n1000=30\n500=40\n200=10\n100=100\n50=0\n");
            Files.writeString(Paths.get(FILE_CASHBOX), sb.toString(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        // создать пустые сеансовые логи
        Files.writeString(Paths.get(OPS_SESSION.get()), "", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        Files.writeString(Paths.get(CASH_SESSION.get()), "", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    static void loadData() throws IOException {
        // accounts
        for (String line : Files.readAllLines(Paths.get(FILE_ACCOUNTS))) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split(";");
            String acc = p[0].trim();
            long rub = Long.parseLong(p[1].trim());
            ACCOUNTS.put(acc, rub * 100);
        }
        // cards
        for (String line : Files.readAllLines(Paths.get(FILE_ACCESS))) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split(";");
            String card = p[0].trim();
            String pin  = p[1].trim();
            String acc  = p[2].trim();
            String role = p[3].trim().toUpperCase();
            String owner= p.length>4 ? p[4].trim() : "N/A";
            CARDS.put(card, new String[]{pin, acc, role, owner});
        }
        // cash
        for (int d : DENOMS) CASH.put(d, 0);
        for (String line : Files.readAllLines(Paths.get(FILE_CASHBOX))) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split("=");
            int denom = Integer.parseInt(p[0].trim());
            int cnt   = Integer.parseInt(p[1].trim());
            CASH.put(denom, cnt);
        }
    }

    static void saveCashbox() throws IOException {
        StringBuilder sb = new StringBuilder("# denom=count\n");
        for (int d : DENOMS) {
            sb.append(d).append("=").append(CASH.getOrDefault(d,0)).append("\n");
        }
        Files.writeString(Paths.get(FILE_CASHBOX), sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    static void saveAccounts() throws IOException {
        StringBuilder sb = new StringBuilder("# accountId;balanceRub\n");
        for (Map.Entry<String, Long> e : ACCOUNTS.entrySet()) {
            sb.append(e.getKey()).append(";").append(e.getValue()/100).append("\n");
        }
        Files.writeString(Paths.get(FILE_ACCOUNTS), sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /* ===================== Логирование ===================== */
    static void logOp(String user, String op, String payload, String result) {
        try {
            String line = String.format("%s | user=%s | op=%s | body=%s | result=%s%n",
                    nowTs(), user, op, payload, result);
            Files.write(Paths.get(OPS_SESSION.get()), line.getBytes(), StandardOpenOption.APPEND);
        } catch (Exception ignored) {}
    }
    static void logCash(String op, int denom, int deltaCount) {
        try {
            String line = String.format("%s | %s | denom=%d | qty=%+d%n", nowTs(), op, denom, deltaCount);
            Files.write(Paths.get(CASH_SESSION.get()), line.getBytes(), StandardOpenOption.APPEND);
        } catch (Exception ignored) {}
    }
    static void joinSessionLogsToAll() {
        try {
            Files.createDirectories(Paths.get(DIR_LOGS));
            // ops
            Files.write(Paths.get(OPS_ALL), Files.readAllBytes(Paths.get(OPS_SESSION.get())),
                    Files.exists(Paths.get(OPS_ALL)) ? new OpenOption[]{StandardOpenOption.APPEND} :
                            new OpenOption[]{StandardOpenOption.CREATE});
            // cash
            Files.write(Paths.get(CASH_ALL), Files.readAllBytes(Paths.get(CASH_SESSION.get())),
                    Files.exists(Paths.get(CASH_ALL)) ? new OpenOption[]{StandardOpenOption.APPEND} :
                            new OpenOption[]{StandardOpenOption.CREATE});
        } catch (Exception ignored) {}
    }

    /* ===================== Выдача наличных: возможность + минимум купюр ===================== */
    // Возвращает план {denom -> count} с минимальным числом купюр при ограниченной кассете.
    // Если невозможно — возвращает пустую Map.
    static Map<Integer,Integer> calcWithdrawPlanMinNotes(long amountRub) {
        Map<Integer,Integer> best = new LinkedHashMap<>();
        long[] bestCount = {Long.MAX_VALUE};
        Map<Integer,Integer> current = new LinkedHashMap<>();
        dfsMake(0, amountRub, 0, best, bestCount, current);
        return best;
    }
    static void dfsMake(int idx, long remain, long used, Map<Integer,Integer> best, long[] bestCount, Map<Integer,Integer> cur) {
        if (remain == 0) {
            if (used < bestCount[0]) {
                bestCount[0] = used;
                best.clear();
                best.putAll(cur);
            }
            return;
        }
        if (idx >= DENOMS.length) return;
        int d = DENOMS[idx];
        int have = CASH.getOrDefault(d,0);
        int max = (int)Math.min(have, remain / d);
        // перебираем от большего количества к меньшему — быстрее найдём минимум по купюрам
        for (int take = max; take >= 0; take--) {
            long nextRemain = remain - (long)take*d;
            long nextUsed   = used + take;
            if (nextUsed >= bestCount[0]) continue; // отсечение
            if (take > 0) cur.put(d, take); else cur.remove(d);
            dfsMake(idx+1, nextRemain, nextUsed, best, bestCount, cur);
            if (bestCount[0] == nextUsed && nextRemain == 0) return; // оптимально найдено на этой ветке
        }
    }

    /* ===================== Псевдографика отчётов ===================== */
    static void printCashReport(boolean saveToFile) {
        StringBuilder out = new StringBuilder();
        String h = "┌──────────────────────────────────────────────┐\n";
        String f = "└──────────────────────────────────────────────┘\n";
        out.append(h);
        out.append(String.format("│ %-44s │%n", "ОТЧЁТ ПО КАССЕТЕ (номинал/кол-во/сумма)"));
        out.append("├───────────┬────────┬─────────────────────────┤\n");
        long total = 0;
        for (int d : DENOMS) {
            int c = CASH.getOrDefault(d,0);
            long s = (long)d * c;
            total += s;
            out.append(String.format("│ %7d ₽ │ %6d │ %11d ₽              │%n", d, c, s));
        }
        out.append("├───────────┴────────┴─────────────────────────┤\n");
        out.append(String.format("│ %-44s │%n", "ИТОГО НАЛИЧНЫХ: " + total + " ₽"));
        out.append(f);

        System.out.print(out.toString());
        if (saveToFile) {
            try {
                String fn = Paths.get(DIR_LOGS, "cash_report_" + SESSION_TAG + ".txt").toString();
                Files.writeString(Paths.get(fn), out.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                System.out.println("Сохранено в файл: " + fn);
            } catch (Exception e) {
                System.out.println("Не удалось сохранить отчёт: " + e.getMessage());
            }
        }
    }

    /* ===================== Диалоговые функции ===================== */
    static final Scanner SC = new Scanner(System.in);
    static String ask(String p) { System.out.print(p + ": "); return SC.nextLine().trim(); }
    static long askLongRub(String p) {
        while (true) {
            String s = ask(p);
            try { long v = Long.parseLong(s); if (v>0) return v; } catch (Exception ignored){}
            System.out.println("Введите положительное целое число.");
        }
    }
    static Map<Integer,Integer> askCashBundle(String title) {
        System.out.println(title + " — введите количество купюр по номиналам (пусто=0):");
        Map<Integer,Integer> m = new LinkedHashMap<>();
        for (int d : DENOMS) {
            String s = ask(d + " ₽ x");
            if (s.isEmpty()) continue;
            try {
                int n = Integer.parseInt(s);
                if (n>0) m.put(d, n);
            } catch (Exception ignored) {}
        }
        return m;
    }

    /* ===================== Операции ===================== */
    static void doWithdraw(String card, String owner, String accId) {
        long rub = askLongRub("Сумма, ₽");
        Map<Integer,Integer> plan = calcWithdrawPlanMinNotes(rub);
        if (plan.isEmpty()) {
            System.out.println("Нельзя выдать точную сумму имеющимися купюрами.");
            logOp(owner, "WITHDRAW", "amount="+rub, "FAIL:no-combination");
            return;
        }
        // проверка баланса
        long bal = ACCOUNTS.getOrDefault(accId, 0L);
        long needK = rub*100;
        if (bal < needK) {
            System.out.println("Недостаточно средств на счёте.");
            logOp(owner, "WITHDRAW", "amount="+rub, "FAIL:no-funds");
            return;
        }

        System.out.println("Будет выдано (минимум купюр):");
        for (Map.Entry<Integer,Integer> e : plan.entrySet()) {
            System.out.printf("  %d x %d%n", e.getKey(), e.getValue());
        }
        String ok = ask("Подтвердить (y/n)");
        if (!ok.toLowerCase().startsWith("y")) { System.out.println("Отменено."); return; }

        // списываем деньги и кассету
        ACCOUNTS.put(accId, bal - needK);
        for (Map.Entry<Integer,Integer> e : plan.entrySet()) {
            CASH.put(e.getKey(), CASH.get(e.getKey()) - e.getValue());
            logCash("WITHDRAW", e.getKey(), -e.getValue());
        }
        try { saveCashbox(); saveAccounts(); } catch (Exception ignored) {}

        System.out.println("Заберите наличные. Баланс: " + (ACCOUNTS.get(accId)/100) + " ₽");
        logOp(owner, "WITHDRAW", "amount="+rub+";notes="+plan, "OK");
    }

    static void doDeposit(String card, String owner, String accId) {
        Map<Integer,Integer> put = askCashBundle("Внесение наличных");
        long sumRub = 0;
        for (Map.Entry<Integer,Integer> e : put.entrySet()) sumRub += (long)e.getKey() * e.getValue();
        if (sumRub <= 0) { System.out.println("Ничего не внесено."); return; }

        long bal = ACCOUNTS.getOrDefault(accId, 0L);
        ACCOUNTS.put(accId, bal + sumRub*100);
        for (Map.Entry<Integer,Integer> e : put.entrySet()) {
            CASH.put(e.getKey(), CASH.get(e.getKey()) + e.getValue());
            logCash("DEPOSIT", e.getKey(), e.getValue());
        }
        try { saveCashbox(); saveAccounts(); } catch (Exception ignored) {}

        System.out.println("Зачислено: " + sumRub + " ₽. Баланс: " + (ACCOUNTS.get(accId)/100) + " ₽");
        logOp(owner, "DEPOSIT", "amount="+sumRub+";notes="+put, "OK");
    }

    static void doPayment(String owner, String accId) {
        String service = ask("Наименование услуги");
        long rub = askLongRub("Сумма, ₽");
        long bal = ACCOUNTS.getOrDefault(accId, 0L);
        long needK = rub*100;
        if (bal < needK) {
            System.out.println("Недостаточно средств.");
            logOp(owner, "PAYMENT", "service="+service+";amount="+rub, "FAIL:no-funds");
            return;
        }
        ACCOUNTS.put(accId, bal - needK);
        try { saveAccounts(); } catch (Exception ignored) {}
        System.out.printf("Оплачено \"%s\" на сумму %d ₽. Баланс: %d ₽%n", service, rub, ACCOUNTS.get(accId)/100);
        logOp(owner, "PAYMENT", "service="+service+";amount="+rub, "OK");
    }

    static void doServiceRefill(String staff) {
        Map<Integer,Integer> add = askCashBundle("Пополнение кассет");
        long total = 0;
        for (Map.Entry<Integer,Integer> e : add.entrySet()) {
            CASH.put(e.getKey(), CASH.get(e.getKey()) + e.getValue());
            logCash("REFILL", e.getKey(), e.getValue());
            total += (long)e.getKey()*e.getValue();
        }
        try { saveCashbox(); } catch (Exception ignored) {}
        System.out.println("Кассеты пополнены на " + total + " ₽.");
        logOp(staff, "SERVICE_REFILL", "notes="+add, "OK");
    }

    static void doServiceReport(String staff) {
        printCashReport(true);
        logOp(staff, "SERVICE_REPORT", "-", "OK");
    }

    /* ===================== Авторизация и меню ===================== */
    static void userSession(String card, String[] info) {
        String owner = info[3];
        String accId = info[1];
        while (true) {
            long balRub = ACCOUNTS.getOrDefault(accId, 0L)/100;
            System.out.printf("%nКлиент: %s | Баланс: %d ₽%n", owner, balRub);
            System.out.println("1) Снять наличные");
            System.out.println("2) Внести наличные");
            System.out.println("3) Оплата услуг");
            System.out.println("4) Отчёт по кассете (просмотр)");
            System.out.println("5) Извлечь карту");
            String c = ask("Выбор");
            if ("1".equals(c)) doWithdraw(card, owner, accId);
            else if ("2".equals(c)) doDeposit(card, owner, accId);
            else if ("3".equals(c)) doPayment(owner, accId);
            else if ("4".equals(c)) printCashReport(false);
            else if ("5".equals(c)) { System.out.println("Карта извлечена."); break; }
            else System.out.println("Неверный выбор.");
        }
    }
    static void serviceSession(String[] info) {
        String staff = info[3];
        while (true) {
            System.out.println("\n*** СЕРВИСНОЕ МЕНЮ ***");
            System.out.println("1) Пополнить кассеты");
            System.out.println("2) Отчёт по кассете (псевдографика) + сохранить");
            System.out.println("3) Извлечь карту");
            String c = ask("Выбор");
            if ("1".equals(c)) doServiceRefill(staff);
            else if ("2".equals(c)) doServiceReport(staff);
            else if ("3".equals(c)) { System.out.println("Сервисная карта извлечена."); break; }
            else System.out.println("Неверный выбор.");
        }
    }

    /* ===================== MAIN ===================== */
    public static void main(String[] args) {
        try {
            ensureFilesExist();
            loadData();
        } catch (Exception e) {
            System.out.println("Ошибка инициализации: " + e.getMessage());
            return;
        }

        System.out.println("Добро пожаловать в ATM (консоль).");
        outer:
        while (true) {
            System.out.println("\n1) Вставить карту  2) Выход");
            String cmd = ask("Выбор");
            if ("2".equals(cmd)) break;
            if (!"1".equals(cmd)) continue;

            String card = ask("Номер карты");
            String[] info = CARDS.get(card);
            if (info == null) {
                System.out.println("Карта не распознана.");
                logOp("UNKNOWN", "AUTH", "card="+card, "FAIL:no-card");
                continue;
            }
            boolean ok = false;
            for (int i=0;i<3;i++) {
                String pin = ask("PIN");
                if (pin.equals(info[0])) { ok = true; break; }
                System.out.println("Неверный PIN.");
            }
            if (!ok) {
                System.out.println("Карта заблокирована (симуляция).");
                logOp(info[3], "AUTH", "card="+card, "FAIL:pin");
                continue;
            }
            logOp(info[3], "AUTH", "card="+card, "OK");

            if ("SERVICE".equalsIgnoreCase(info[2])) serviceSession(info);
            else userSession(card, info);
        }

        // финал: сохранить и склеить журналы
        try {
            saveAccounts();
            saveCashbox();
            joinSessionLogsToAll();
        } catch (Exception ignored) {}

        System.out.println("До свидания!");
    }
}
