package bot;

import java.awt.List;
import java.util.ArrayList;
import java.util.Random;

import rts.UnitAction;
import rts.units.Unit;

public class MCUnitActions
{
	Unit unit;
	ArrayList<UnitAction> ValidActions;
	ArrayList<UnitAction> ActionsPermant;
	Random r;
	public MCUnitActions(Unit u, ArrayList<UnitAction> UAL)
	{
		unit = u;
		ValidActions = (ArrayList<UnitAction>) UAL.clone();
		ActionsPermant = (ArrayList<UnitAction>) UAL.clone();
		r = new Random();
	}
	public UnitAction GetBestAction()
	{
		
		for(int i =0; i < ValidActions.size();)
		{
			switch(ValidActions.get(i).getType())
			{
			  case UnitAction.TYPE_ATTACK_LOCATION:
				  return ValidActions.remove(i);
				  
			  case UnitAction.TYPE_RETURN:
				  return ValidActions.remove(i);

			  case UnitAction.TYPE_HARVEST:
				  return ValidActions.remove(i);

			  case UnitAction.TYPE_PRODUCE:
				  return ValidActions.remove(i);
			}
			i++;
		}
		return null;
		
	}
	
	public void Reset()
	{
		ValidActions = (ArrayList<UnitAction>)ActionsPermant.clone();
	}
	public UnitAction GetRandomAction()
	{

		if(!ValidActions.isEmpty())
		{
			return ValidActions.remove(r.nextInt(ValidActions.size()));
		}
		else return new UnitAction(UnitAction.TYPE_NONE);
	}
}
