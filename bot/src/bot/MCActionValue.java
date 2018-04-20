package bot;

import rts.PlayerAction;

public class MCActionValue
{
	private PlayerAction PA = null;
	private double Value = 0;
	
	public MCActionValue(PlayerAction pa, double value ) 
	{
		PA = pa;
		Value = value;	
	}
	public double GetValue()
	{
		return Value;
	}
	public PlayerAction GetPlayerAction()
	{
		return PA;
	}
	
}
