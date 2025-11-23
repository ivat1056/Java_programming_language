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

    public void setCount(int count) 
    {
        this.count = count;
    }

    // 
    public void addCash(int amount) throws InvalidDenominationException 
    {
        // 
        if (amount < 0) 
        {
            throw new InvalidDenominationException("Количество купюр не может быть отрицательным");
        }
        this.count += amount;
    }

    // 
    public void removeCash(int amount) throws InsufficientDenominationException 
    {
        // 
        if (amount > this.count) 
        {
            throw new InsufficientDenominationException(
                "Недостаточное количество купюр номинала " + denomination + 
                ". Требуется: " + amount + ", доступно: " + this.count
            );
        }
        this.count -= amount;
    }
}