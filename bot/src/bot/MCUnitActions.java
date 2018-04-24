package bot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import rts.UnitAction;
import rts.units.Unit;

/**
 * This class stores all the possible actions each unit unit can do.
 * In addition stores the amount of visits, the evaluation and its probability of being chosen
 * weighting for each action a unit has.
 */
public class MCUnitActions
{
	//This is used to skew the best action a bit, otherwise the
	//chances of picking the best action are greatly reduced
	static double EpsilonL = 0.3;
	
	//The unit that the action array belongs to
	private Unit unit;
	
	//This is a list actions that the unit can do, the order of the actions is important
	//as it is used to manipulate its vists, evaluation and weighting
	private ArrayList<UnitAction> actions;
	
	//This is the number of actions the unit can do.
	private int numberOfActions;
	
	//An array that stores the evaluation of the unit action that shares the same
	//index in the Action array list. 
    private double[] actionEvaluations = null;
    
	//An array that stores the visits of the unit action that shares the same
	//index in the Action array list.
    private int[] actionVisits = null;
    
	//An array that stores the weighting of the unit action that shares the same
	//index in the Action array list.
    private double[] actionWeights = null;
    
    //When picking an action, I add the action weights and their corresponding index into a map
    //I use this to keep track of the actions there is left to sample.
    private Map<Integer, Double> unsampledActions = null;
    
    //random used for the sampler.
	private Random r = new Random();
	
	/**
	 * This constructor sets the member variables for this class
	 * in addition it initialises the various arrays and lists. 
	 * @param u   The unit that owns the actions coming in
	 * @param uas The list of unit actions that the corresponding unit can make
	 */
	public MCUnitActions(Unit u, ArrayList<UnitAction> uas)
	{
		unit = u;
		actions = uas;
		numberOfActions = actions.size();
		actionEvaluations = new double[numberOfActions];
		actionVisits = new int[numberOfActions];
		actionWeights = new double[numberOfActions];
		unsampledActions = new HashMap<Integer, Double>();

		//Cycles through all the arrays and sets their default values to 0;
		for(int i = 0; i < numberOfActions; i++)
		{
			actionEvaluations[i] = 0;
			actionVisits[i] = 0;
			actionWeights[i] = 0;
		}
	}
	
	/**
	 * Used during propagation, it will update the evaluation and visit
	 * scores of the action UA.
	 * @param ua   The unit action to be updates
	 * @param eval The eval score to be assigned to the unit actions
	 */
	public void UpdateAction(UnitAction ua,double eval)
	{
		//get, add, increment.
		int i = actions.indexOf(ua);
		actionEvaluations[i] += eval;
		actionVisits[i]++;
	}
	
	/**
	 * Go through all the action weights that correspond to their actions by index.
	 * Set their value using their evaluation and visit values.
	 * @param IsMinPlayer used to for the player switch, as evaluations for min player are negative
	 */
	public void CalculateActionWeights(boolean IsMinPlayer)
	{
		
		int bestIndex = -1;
		int visits = 0;
		double bestEvaluation = 0;
		int PlayerSwitch = 1;
		
		//player switch
		if(IsMinPlayer) PlayerSwitch = -1;
		
		//Cycle through all the actions and evaluate their action weights
		//This bit is based on some sample code, adapted though
		for(int i = 0; i<numberOfActions; i++)
        {
			
     	    double AverageActionEvaluation = actionEvaluations[i]/actionVisits[i]*PlayerSwitch;
            if (bestIndex==-1 || 
               (visits!=0 && actionVisits[i]==0) ||
               (visits!=0 && (AverageActionEvaluation)>bestEvaluation))
            {
            		bestIndex = i;
                    if (actionVisits[i]>0)
                    {
                 	   bestEvaluation = (AverageActionEvaluation);
	                   }
                    else
             	   { 
                 	   bestEvaluation = 0;
             	   }
                    
                    visits = actionVisits[i];
            }
            actionWeights[i] = EpsilonL/numberOfActions;
        }
		
		//populate 
       SetUnsampledActions();
	}
	
	/**
	 * Populate the unsampled actions using the action weights and their indexs. 
	 */
	private void SetUnsampledActions()
	{
		//clear to just to make sure we start afresh
		unsampledActions.clear();
		int index = 0;
		for(double d: actionWeights)
		{
			unsampledActions.put(index, d);
			index++;
		}
	}
	
	/**
	 * Probably the most important function. It will return an actions, based on if it hasnt already been sampled
	 * and based on random chance, with the moves with higher average evaluation being more likely
	 * but not guaranteed 
	 * @return Unitaction that was semi randomly chosen. 
	 */
	public UnitAction GetWeightedAction()
	{
		//quick check
		if(unsampledActions.size() == 0)return null;
		
		//loop through unsampled actions and add their weighting to the total.
		double total = 0;
		int[] indexs = new int[unsampledActions.size()];
		int i = 0;
		
		for(int index: unsampledActions.keySet())
		{
			total += unsampledActions.get(index);
			indexs[i] = index;
			i++;
		}
		
		int index = 0;
		
		//If none of the actions have any evaluation set the return index
		//to a random action.
		if(total == 0)
		{
			index = indexs[r.nextInt(indexs.length)];
		}
		else
		{
			//Got this code from the inbuild java sampler class
			//wasnt quite what i needed so i edited to work for dicts.
			double accum =0;
	        double tmp = r.nextDouble() * total;
	        for (int j = 0; j < indexs.length; j++) {
	            accum += unsampledActions.get(indexs[j]);
	            if (accum >= tmp) 
	            {
	                index = indexs[j];
	                break;
	            }
	        }
		}
		
		//finally remove the chosen index from the unsampled ones and return the action.
		unsampledActions.remove(index);
		return actions.get(index);

	}

	public Unit GetUnit()
	{
		return unit;
	}
	
	
}