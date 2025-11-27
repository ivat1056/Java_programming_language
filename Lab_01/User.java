// 
public class User 
{
    private String name;
    private String role;
    private int balance;

    public User(String name, String role, int balance) 
    {
        this.name = name;
        this.role = role;
        this.balance = balance;
    }

    public String getName() { return name; }
    public String getRole() { return role; }
    public int getBalance() { return balance; }
    public void setBalance(int balance) { this.balance = balance; }
}