public class Operation 
{
    private String name;         
    private String details;      
    private String result;       
    private String user;         
    private Journal journal;     

    public Operation(String name, String details, String user, Journal journal) 
    {
        this.name = name;
        this.details = details;
        this.user = user;
        this.journal = journal;
    }

    public void execute(String result) 
    {
        this.result = result;
        log();
    }

    private void log() 
    {
        journal.log(name, details, result, user);
    }

    public String getName() { return name; }
    public String getDetails() { return details; }
    public String getResult() { return result; }
    public String getUser() { return user; }
}   