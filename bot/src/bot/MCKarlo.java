/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import ai.core.AI;
import ai.RandomBiasedAI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import rts.GameState;
import rts.PlayerAction;
import rts.PlayerActionGenerator;
import rts.units.UnitTypeTable;
import ai.core.InterruptibleAI;
import ai.portfolio.*;
import ai.minimax.*;
import ai.abstraction.*;
import ai.abstraction.pathfinding.GreedyPathFinding;
import ai.RandomAI;
import java.util.Map;
import ai.abstraction.cRush.*;


/**
 *
 * @author santi
 */
public class MCKarlo extends AIWithComputationBudget implements InterruptibleAI 
{
	EvaluationFunction EvaluationMethod;
	AI BaseAI;

	List<PlayerAction> GoodActions;
	Map<Float, GameState> Plays = new HashMap<Float, GameState>();
	
	int Depth = 10;
	int Breadth = 10;
	int MinPlayer = 1;
	int MaxPlayer =0;
	int RunsThisMove = 0;
	int TimeBudget = 0;
	int LookaHead = 100;
	int TotalPlayouts = 0;
	
	MCNode root = null;
	GameState StartGameState;

	boolean ComputationComplete = true;
	
	PlayerAction FinalAction;
	ArrayList<GameState> States;
	AI BigMapPolicy;
	
    public MCKarlo(UnitTypeTable utt) 
    {
        this(100,-1, 50,20, new RandomBiasedAI(utt), new SimpleSqrtEvaluationFunction3());
        BigMapPolicy = new CRush_V2(utt,new GreedyPathFinding());
    }

    public MCKarlo(int available_time, int MaxPlayouts, int breadth, int depth, AI AIPolicy, EvaluationFunction a_ef) 
    {
        super(available_time, MaxPlayouts);
        Depth = depth;
        Breadth =breadth;
        BaseAI = AIPolicy;
        EvaluationMethod = a_ef;

    }
    
    public final PlayerAction getAction(int player, GameState gs) throws Exception
    {	

    	MaxPlayer = player;
    	if(MaxPlayer ==1)MinPlayer =0;
    	else MinPlayer =1;
    	
        if ((gs.getPhysicalGameState().getWidth() *gs.getPhysicalGameState().getHeight()) >= 144) {
           return BigMapPolicy.getAction(player, gs);
        }

    	if(gs.canExecuteAnyAction(player))
    	{
    		startNewComputation(player, gs);
    		ComputeFillFirst();
    		//computeDuringOneGameFrame();
    		return getBestActionSoFar();
    	}
    	else return BaseAI.getAction(MaxPlayer, StartGameState);
    }

	@Override
	public void startNewComputation(int player, GameState gs) throws Exception
	{
	 	long start = System.currentTimeMillis();
        long cutOffTime = start +  TIME_BUDGET;
		StartGameState = gs;
		root = new MCNode(player, gs.clone(), Depth, Breadth, cutOffTime);
	}
	public void ComputeFillFirst() throws Exception
	{
	 	long start = System.currentTimeMillis();
        int nPlayouts = 0;
        int numberOfNodes = 0;
        long cutOffTime = start +  TIME_BUDGET;
        long lastIterationTime = 0;
        boolean Compute = true;
        MCNode node;
        int tDepth = 0;
        while(Compute) 
        {
        	long currentTime = System.currentTimeMillis();
            if (cutOffTime >0 && currentTime> cutOffTime) break;
            try {node = root.GetChild();tDepth = node.Depth;}
            catch(NullPointerException e){break;}

            double Eval  = 0;

            while(node != null)
            {
            	GameState gs2 = node.GSCopy.clone();
            	SimulateGame(gs2, gs2.getTime() + LookaHead );
                int time = gs2.getTime() - StartGameState.getTime();
            	Eval  = EvaluationMethod.evaluate(MaxPlayer, MinPlayer, gs2)*Math.pow(0.99,time/tDepth);
            	//System.out.println(Eval);
            	node.wins += Eval;
            	node.visits++;
            	node = node.ParentNode;
            }
            lastIterationTime = System.currentTimeMillis() - currentTime;
            //System.out.println(lastIterationTime);
			nPlayouts++;
		}
	}

	@Override
	public void computeDuringOneGameFrame() throws Exception
	{
		 	long start = System.currentTimeMillis();
	        int nPlayouts = 0;
	        int numberOfNodes = 0;
	        long cutOffTime = start +  TIME_BUDGET;
	        long lastIterationTime = 0;
	        boolean Compute = true;
	        while(Compute) 
	        {
	        	long currentTime = System.currentTimeMillis();
	            if (cutOffTime >0 && currentTime> cutOffTime) break;
    			MCNode node = root;
    			
    			while(node.ChildNodes.size() > 0  && node.UntriedMoves.size() < 1)
    			{
    				node = node.GetChild();
    				numberOfNodes++;
    			}
    			if(node.UntriedMoves != null && node.UntriedMoves.size() > 0)
    			{
    				node = node.AddChild(Depth, cutOffTime);
    			}
	            while(node != null)
	            {
	            	GameState gs2 = node.GSCopy.clone();
	            	SimulateGame(gs2, gs2.getTime() + LookaHead );
	                int time = gs2.getTime() - StartGameState.getTime();
	            	double Eval  = EvaluationMethod.evaluate(MaxPlayer, MinPlayer, gs2)*Math.pow(0.99,time/Depth);
	            	node.Update(Eval);
	            	node = node.ParentNode;
	            }
	            lastIterationTime = System.currentTimeMillis() - currentTime;
	            System.out.println(lastIterationTime);
    			nPlayouts++;
    		}

        }

	@Override
	public PlayerAction getBestActionSoFar() throws Exception
	{
        if (root.ChildNodes.size() < 1) 
        {
            return BaseAI.getAction(MaxPlayer, StartGameState);
        }
        else 
        { 
            
        	return root.GetMostVisitedNode().Move;
        }
        
	}

	@Override
	public void reset()
	{
        StartGameState= null;
        root = null;
        RunsThisMove = 0;
	}

	@Override
	public AI clone()
	{
		// TODO Auto-generated method stub
		return new MCKarlo(TimeBudget, -1, Breadth, Depth, BaseAI, EvaluationMethod);
	}
	
	@Override
	public List<ParameterSpecification> getParameters()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	public void SimulateGame(GameState gs, int time)throws Exception 
	{

        boolean gameover = false;

        do{
            if (gs.isComplete()) 
            {
                gameover = gs.cycle();
            } 
            else 
            {
                gs.issue(BaseAI.getAction(MaxPlayer, gs));
                gs.issue(BaseAI.getAction(MinPlayer, gs));
            }
        }while(!gameover && gs.getTime()<time);   
		
	}  
	public boolean SimulateMove(GameState Sgs, PlayerAction move)throws Exception
	{
		Sgs.issue(move);
		Sgs.issue(BaseAI.getAction(MinPlayer, Sgs));
		Sgs.cycle();
		if(Sgs.isComplete())return true;
		return false;
	}
}
