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

import javax.naming.ldap.StartTlsRequest;

import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.PlayerActionGenerator;
import rts.units.Unit;
import rts.units.UnitTypeTable;
import ai.core.InterruptibleAI;
import ai.portfolio.*;
import ai.minimax.*;
import ai.abstraction.*;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.GreedyPathFinding;
import ai.RandomAI;
import java.util.Map;
import ai.abstraction.cRush.*;
import ai.abstraction.*;


/**
 *
 * @author santi
 */
public class MCKarlo extends AbstractionLayerAI implements InterruptibleAI 
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
	int LookaHead = 50;
	int TotalPlayouts = 0;
	boolean IsStuck = false;
	
	MCNode root = null;
	GameState StartGameState;

	boolean ComputationComplete = true;
	
	PlayerAction FinalAction;
	ArrayList<GameState> States;
	AI BigMapPolicy;
	AI LateGamePolicy;
	
    public MCKarlo(UnitTypeTable utt) 
    {
        this(100,-1, 1000, 15, new RandomBiasedAI(utt), new SimpleSqrtEvaluationFunction3());
        BigMapPolicy = new PortfolioAI(utt);
        LateGamePolicy = new WorkerRush(utt,new GreedyPathFinding());
    }

    public MCKarlo(int available_time, int MaxPlayouts, int breadth, int depth, AI AIPolicy, EvaluationFunction a_ef) 
    {
        super(new AStarPathFinding(), available_time, MaxPlayouts);
        Depth = depth;
        Breadth =breadth;
        BaseAI = AIPolicy;
        EvaluationMethod = a_ef;
    }
    
    public void ChangeInputParams(int breadth, int depth, int looka)
    {
    	Breadth = breadth;
    	Depth = depth; 
    	LookaHead = looka;
    	
    }
    
    public final PlayerAction getAction(int player, GameState gs) throws Exception
    {	

    	MaxPlayer = player;
    	if(MaxPlayer ==1)MinPlayer =0;
    	else MinPlayer =1;
    	
    	if(gs.getPhysicalGameState().getWidth()<= 8 && gs.getTime() < 1500 )
    	{
    		//return LateGamePolicy.getAction(player, gs);
    	}
    	if(gs.canExecuteAnyAction(player) && gs.getTime() < 4500)
    	{
    		startNewComputation(player, gs);
    		computeDuringOneGameFrame();
    		return getBestActionSoFar();
    	}
    	if(IsStuck) return StuckGameRush();
    	else return BaseAI.getAction(player, gs);
    }

	@Override
	public void startNewComputation(int player, GameState gs) throws Exception
	{
	 	long start = System.currentTimeMillis();
        long cutOffTime = start +  TIME_BUDGET;
		StartGameState = gs;
		root = new MCNode(MaxPlayer, MinPlayer, gs.clone(), Depth, Breadth, cutOffTime);
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
        MCNode node;
        int tDepth = 0;
        while(Compute) 
        {
        	long currentTime = System.currentTimeMillis();
            if (cutOffTime >0 && currentTime> cutOffTime) break;
            try 
            {
            	node = root.GetChild(MaxPlayer, MinPlayer);
            	tDepth = node.Depth;
            	//System.out.println("Depth : " + tDepth);
            }
            catch(NullPointerException e)
            {
            	break;
            }

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
           // lastIterationTime = System.currentTimeMillis() - currentTime;
            //System.out.println(lastIterationTime);
			nPlayouts++;
		}
     // System.out.println("Playouts : "+nPlayouts);
 }

	@Override
	public PlayerAction getBestActionSoFar() throws Exception
	{
        if (root.ChildNodes.size() <= 1) 
        {
        	return BigMapPolicy.getAction(MaxPlayer, StartGameState);
        }
        else 
        { 
            MCNode n = root.GetBestNode();
        	return n.Move;
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
	
    public Unit getNearestEnemy(PhysicalGameState pgs, Player p, Unit u)
    {
    	 Unit closestEnemy = null;
         int closestDistance = 0;
         for(Unit u2:pgs.getUnits()) 
         {
             if (u2.getPlayer()>=0 && u2.getPlayer()!=p.getID()) { 
                 int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                 if (closestEnemy==null || d<closestDistance) {
                     closestEnemy = u2;
                     closestDistance = d;
                 }
             }
         }
         if (closestEnemy!=null)
         {
             return closestEnemy;
         }
         else
         {
        	 return null;
         }
    }
    
    public PlayerAction AllAttackNearestEnemy(Player p, GameState gs, List<Unit> UL)
    {
    	for(Unit u : UL)
    	{
        PhysicalGameState pgs = gs.getPhysicalGameState();
        attack(u, getNearestEnemy(pgs, p, u));
    	}
    	return translateActions(MaxPlayer, gs);
         
    }
    
    public PlayerAction StuckGameRush()
    {
    	List<Unit> UL = new ArrayList<Unit>();
    	for(Unit u: StartGameState.getPhysicalGameState().getUnits())
    	{
    		if(u.getPlayer() == MaxPlayer && u.getType().canAttack)
    		{
    			UL.add(u);
    		}
    	}
    	return AllAttackNearestEnemy(StartGameState.getPlayer(MaxPlayer), StartGameState, UL);
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
	
	//Found online at https://stackoverflow.com/questions/16656651/does-java-have-a-clamp-function
	public static float clamp(float val, float min, float max) {
	    return Math.max(min, Math.min(max, val));
	}

	
}
