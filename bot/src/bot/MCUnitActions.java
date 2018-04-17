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
	public MCUnitActions(Unit u, ArrayList<UnitAction> UAL)
	{
		unit = u;
		ValidActions = UAL;
	}
	public UnitAction GetBestAction()
	{
		for(UnitAction UA : ValidActions)
		{
			if(UA.getType() == UnitAction.TYPE_ATTACK_LOCATION) return UA;
			if(UA.getType() == UnitAction.TYPE_RETURN) return UA;
			if(UA.getType() == UnitAction.TYPE_HARVEST) return UA;
			if(UA.getType() == UnitAction.TYPE_PRODUCE) return UA;
		}
		if(!ValidActions.isEmpty())return ValidActions.get(0);
		else return null;
		
	}
	public UnitAction GetRandomAction()
	{
		Random r = new Random();
		if(!ValidActions.isEmpty())
		{
			return ValidActions.get(r.nextInt(ValidActions.size()));
		}
		else return null;
	}
}
