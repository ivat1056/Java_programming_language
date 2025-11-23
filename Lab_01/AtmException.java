public class AtmException extends Exception 
{
    public AtmException(String message) 
    {
        super(message);
    }
}

class InvalidDenominationException extends AtmException 
{
    public InvalidDenominationException(String message) 
    {
        super(message);
    }
}

class InsufficientCashException extends AtmException 
{
    public InsufficientCashException(String message) 
    {
        super(message);
    }
}

class InsufficientDenominationException extends AtmException 
{
    public InsufficientDenominationException(String message) 
    {
        super(message);
    }
}

class InvalidOperationException extends AtmException 
{
    public InvalidOperationException(String message) 
    {
        super(message);
    }
}

class InvalidOperationParametersException extends AtmException 
{
    public InvalidOperationParametersException(String message) 
    {
        super(message);
    }
}

class InvalidCredentialsException extends AtmException 
{
    public InvalidCredentialsException(String message) 
    {
        super(message);
    }
}