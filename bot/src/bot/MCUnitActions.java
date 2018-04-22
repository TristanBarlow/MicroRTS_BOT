package bot;

import java.awt.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import rts.UnitAction;
import rts.units.Unit;
import util.Sampler;

public class MCUnitActions
{
	static double EpsilonL = 0.3;
	
	private Unit unit;
	private ArrayList<UnitAction> Actions;
	private int NumberOfActions;
    private double[] ActionEvaluations = null;
    private int[] ActionVisits = null;
    private double[] ActionWeights = null;
    private Map<Integer, Double> UnsampledActions = null;
	
	public MCUnitActions(Unit u, ArrayList<UnitAction> uas)
	{
		unit = u;
		
		Actions = uas;
		NumberOfActions = Actions.size();
		ActionEvaluations = new double[NumberOfActions];
		ActionVisits = new int[NumberOfActions];
		ActionWeights = new double[NumberOfActions];

		for(int i = 0; i < NumberOfActions; i++)
		{
			ActionEvaluations[i] = 0;
			ActionVisits[i] = 0;
			ActionWeights[i] = 0;
		}
		UnsampledActions = new HashMap<Integer, Double>();
	}
	
	public void UpdateAction(UnitAction ua,double eval)
	{
		int i = Actions.indexOf(ua);
		ActionEvaluations[i] = eval;
		ActionVisits[i]++;
	}
	
	public void CalculateActionWeights(boolean IsMinPlayer)
	{
		int bestIndex = -1;
		int visits = 0;
		double bestEvaluation = 0;
		int PlayerSwitch = 1;
		if(IsMinPlayer) PlayerSwitch = -1;
		
			for(int i = 0; i<NumberOfActions; i++)
	        {
	     	   double AverageActionEvaluation = ActionEvaluations[i]/ActionVisits[i];
	            if (bestIndex==-1 || 
	               (visits!=0 && ActionVisits[i]==0) ||
	               (visits!=0 && (AverageActionEvaluation*PlayerSwitch)>bestEvaluation))
	            {
	            		bestIndex = i;
	                    if (ActionVisits[i]>0)
	                    {
	                 	   bestEvaluation = (AverageActionEvaluation);
		                   }
	                    else
	             	   { 
	                 	   bestEvaluation = 0;
	             	   }
	                    
	                    visits = ActionVisits[i];
	            }
	            ActionWeights[i] = EpsilonL/NumberOfActions;
	        }
           if(ActionVisits[bestIndex] != 0)
           {
        	   ActionWeights[bestIndex] = (1-EpsilonL)+ (EpsilonL/NumberOfActions);
           }
           SetUnsampledActions();
	}
	private void SetUnsampledActions()
	{
		int index = 0;
		for(double d: ActionWeights)
		{
			UnsampledActions.put(index, d);
			index++;
		}
	}
	
	public UnitAction GetWeightedAction()
	{
		if(UnsampledActions.size() == 0)return null;
		
		Random r = new Random();
		double total = 0;
		int[] indexs = new int[UnsampledActions.size()];
		int i = 0;
		for(int index: UnsampledActions.keySet())
		{
			total += UnsampledActions.get(index);
			indexs[i] = index;
			i++;
		}
		
		int index = 0;
		
		if(total == 0)
		{
			index = indexs[r.nextInt(indexs.length)];
		}
		else
		{
			double accum =0;
	        double tmp = r.nextDouble() * total;
	        for (int j = 0; j < indexs.length; j++) {
	            accum += UnsampledActions.get(indexs[j]);
	            if (accum >= tmp) 
	            {
	                index = indexs[j];
	                break;
	            }
	        }
		}
		UnsampledActions.remove(index);
		return Actions.get(index);

	}

	public Unit GetUnit()
	{
		return unit;
	}
	
	
}