public class CashHolder 
{
    private int denomination;
    private int count;

    public CashHolder(int denomination, int count) 
    {
        this.denomination = denomination;
        this.count = count;
    }

    public int getDenomination() 
    {
        return denomination;
    }

    public int getCount() 
    {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void addCash(int amount) 
    {
        this.count += amount;
    }

    public void removeCash(int amount) 
    {
        this.count -= amount;
    }
}