public class Operation 
{
    private String name;         // Название операции
    private String details;      // Описание/детали операции
    private String result;       // Результат выполнения
    private String user;         // Пользователь
    private Journal journal;     // Журнал для записи

    public Operation(String name, String details, String user, Journal journal) 
    {
        this.name = name;
        this.details = details;
        this.user = user;
        this.journal = journal;
    }

    // Вызов исполнения операции и запись результата в журнал
    public void execute(String result) 
    {
        this.result = result;
        log();
    }

    // Запись результата в журнал
    private void log() 
    {
        journal.log(name, details, result, user);
    }

    // Геттеры (если нужно)
    public String getName() { return name; }
    public String getDetails() { return details; }
    public String getResult() { return result; }
    public String getUser() { return user; }
}