package bot;

import ai.core.AI;
import ai.RandomBiasedAI;
import ai.core.ParameterSpecification;
import ai.evaluation.EvaluationFunction;
import java.util.List;

import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.Unit;
import rts.units.UnitTypeTable;
import ai.core.InterruptibleAI;
import ai.abstraction.*;
import ai.abstraction.pathfinding.AStarPathFinding;

/**
 * @author Tristan
 *This bot implements an MCTS algorithm with the main focus
 * being on the individual evaluation of each units action
 * And sampling unit actions based on random chance and previous
 * unit actions evaluations
 */
public class MCNoots extends AbstractionLayerAI implements InterruptibleAI 
{
	//Evaluation Method Used To determine the effectiveness of the input gamestate
	private EvaluationFunction EvaluationClass;
	
	//The AI used in the simulated playouts. The more expensive the Action get the less simulations
	private AI BaseAI;
	
	//Arguably the most important variable. This is the root node that will be used to get access to the rest
	//and be responsible for future nodes being created and where the move that will be used will be stored
	private MCNode root = null;
	
	//This is a copy of the gamestate When the getAction function is called.
	private GameState StartGameState;
	
	//The player We want to lose
	private int MinPlayer = 1;
	
	//The player We want to win
	private int MaxPlayer = 0;
	
	//MaxDepth Dictates the Maximum Depth the Tree can go
	private int MaxDepth = 10;
	
	//How Long the simulations should look ahead to see value of an action
	private int LookaHead = 100;
	
	//When the GameState.time() received from the getAction call goes above this it will trigger the rush.
	private int RushTimer = 4500;
	
	//Reset at The Start time of each computation using a global to reduce the amount of times the current time
	// function is called.
 	private long StartTime = 0;
 	
 	 //Reset at the start of each computation, this is calculated by the start time + the time budget 
   	 private long CutOffTime = 0;
	
        //This is the amount of time in milliseconds the AI is allowed to do its computation;
   	 private int TimeBudget = 0;
    
	//Annoying the AI tried to build barracks on small maps. Which is a bad idea.
	//This will make any unitactions searched through ignore barracks builds.
	private boolean canBuildBarracks = false;
	
	//Used When In the late game to trigger a rush. This is to stop those annoying moments when its winning
	//But can't see far enough into the board to see a win state
	private boolean IsStuck = false;
	
	/**
	 * The Main constructor that is called. However this just calls another constructor where
	 * The initialisation is done.
	 * @param utt
	 */
    public MCNoots(UnitTypeTable utt) 
    {
        this(100, 10, new RandomBiasedAI(utt), new MCEvaluation(utt));
    }
    
    /**
     * The Main constructor for MCNoots where the initialisation of the AIs policies are done
     * @param MaxComutationTime This goes into the parent class and initialises the TIME_BUDGET variable
     * @param depth				Sets the MaxDepth of the Search Tree
     * @param AIPolicy			This Sets the Simulation Playout Policy.
     * @param EF				This sets the Evaluation function used to determine the effectiveness of the actions
     */
    public MCNoots(int MaxComutationTime,int depth, AI AIPolicy, EvaluationFunction EF) 
    {
    	//Parent Initialisation 
        super(new AStarPathFinding());
        
        //MCKarlo Initialisation
        TimeBudget = MaxComutationTime;
        MaxDepth = depth;
        BaseAI = AIPolicy;
        EvaluationClass = EF;
    }
    
    /**
     * This is the main function that is used to get the players next action
     * @param player is the ID of the AIs current player number
     * @param gs     is the current state of the Game.
     */
    public final PlayerAction getAction(int player, GameState gs) throws Exception
    {	
    	
    	//Setting the Max and Min Players, You only know what player you are when you're required to make
    	// a move, I stole the Idea of the 1-player bit from Rich. TY!
    	MaxPlayer = player;
    	MinPlayer = 1-player;
    	
    	//A heuristic for large Maps, this will alter how the AI will playout as on bigger maps
    	// its good at producing but doesn't have the depth to see enemies to attack.
    	if(gs.getPhysicalGameState().getWidth()* gs.getPhysicalGameState().getHeight() > 144 ||gs.getPhysicalGameState().getWidth()*gs.getPhysicalGameState().getHeight() == 72  )
    		{
    			RushTimer = 2000;
    			canBuildBarracks = true;
    		}
    	//This Is where the main computation algorithms are called on the outermost layer
    	// only called if the current game time is less than the rush timer
    	// and it Isn't stuck
    	if(gs.canExecuteAnyAction(player) && !IsStuck && gs.getTime() < RushTimer)
    	{
    		//Refresh node and other variables
    		startNewComputation(player, gs);
    		
    		//Compute until times out
    		computeDuringOneGameFrame();
    		
    		//Return the best it got
    		return getBestActionSoFar();
    	}
    	
    	// If it gets here its either stuck or the timer is up
    	// Making it rush if the timer isn't up can sometimes reset it back to a state it can
    	// start trying to be clever again
    	else if(IsStuck) 
    	{ 
    		IsStuck = false;
    		return StuckGameRush();
    	}
    	
    	//if the timer is up and its not stuck the search and destroy.
    	else return StuckGameRush();
    }
    
    /**
     * Override Function from the inherited Parent. This function will reset the variables used in each computation
     * It needs to be here so I just used it, despite the useless @param
     * @param player not used Just here as The Max and Min Player are determined beforehand.
     * @param gs     used to set the startGameState to keep a constant copy
     */
	public void startNewComputation(int player, GameState gs) throws Exception
	{
		//Get the current time and set the start time for this computation
		StartTime = System.currentTimeMillis();
		
		// Set the time at which this computation needs to end
		CutOffTime = StartTime + TimeBudget;
		
		//Record the starting state of the game
		StartGameState = gs;
		
		//Create the root node, with all the required parameters 
		root = new MCNode(MaxPlayer, MinPlayer, gs.clone(), MaxDepth, canBuildBarracks);
	}


	/**
	 * Override Function from the inherited Parent. This is where the main computing will occur for this frame.
	 * The function will finish when the current time is greater than the CutOffTime
	 */
	public void computeDuringOneGameFrame() throws Exception
	{
		//The number of times it loops through the main Computation while loop
        int nPlayouts = 0;
        
        //Not necessary, Added for readability.
        boolean Compute = true;
        
        while(Compute) 
        {
        	//Get the current time
        	long currentTime = System.currentTimeMillis();
        	
        	//Check to see if the AI is out of time and needs to return a move
            if (CutOffTime > 0 && currentTime> CutOffTime) break;
            
            //The select part of the algorithm, variations of this function are called internally
            //to hopefully provide a more useful node for simulation
        	MCNode node = root.GetChild();
        	
        	//Sanity check to make sure no calls are done if GetChild returns null(it shouldn't)
        	if(node == null)return;

            //GameState Simulation
        	GameState gs2 = node.GetGamestate().clone();
        	SimulateGame(gs2, gs2.getTime() + LookaHead);
        	int time = gs2.getTime() - StartGameState.getTime();
        	
        	
            //After the simulating is done evaluate the state of the game.
        	//The multiplication at the end make sure that My ai does not think its won the moment it sees a terminal
        	//state
            double tEval = EvaluationClass.evaluate(MaxPlayer, MinPlayer, gs2)*Math.pow(0.99,time/10.0);;
            
            //Propagate the evaluation values up the tree inside this function the individual unit move evaluation is assigned.
            while(node != null)
            {
            	node.PropogateValue(tEval);
            	node = node.GetParentNode();
            }
            
            nPlayouts++;
		}
 }

	/**
	 * This is an override function from the inherited parent class. It is called
	 * when the AI has run out of time, proved it has moves to return it will, 
	 * else The BaseAI is used as a return instead.
	 */
	public PlayerAction getBestActionSoFar() throws Exception
	{
		//check to see if the root node has any children. If there is no children then return the BaseAI action
        if (root.GetNumberOfChildren() <= 0) 
        {
        	return BaseAI.getAction(MaxPlayer,StartGameState);
        }
        else 
        {
        	//If there are children get the best node. Using visit first then the evaluation rating of the node. 
            MCNode n = root.GetBestNode();
        	return n.GetNodeMove();
        }
        
	}

	/**
	 * Override Function, I don't use it.
	 */
	public AI clone()
	{
		// TODO Auto-generated method stub
		return new MCNoots(TimeBudget,MaxDepth, BaseAI, EvaluationClass);
	}
	
	/**
	 * Override Function, I don't use it.
	 */
	public List<ParameterSpecification> getParameters()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Gets the nearest Enemy to a given unit for a given player. This is taken from the sample bots
	 * as it is quite trivial and standard i Didn't see a point of writing my own.
	 * I made it static so that I could use it in the MCNode class aswell.
	 * @param pgs the physical gamestate of the board to be examined
	 * @param p the player owning the unit
	 * @param u	the given unit as the reference points
	 * @return returns the unit closest to the unit passed as an argument
	 */
    public static Unit getNearestEnemy(PhysicalGameState pgs, Player p, Unit u)
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

    /**
     * A heuristic based attack. Used late game as the AI, especially on big maps struggle to find targets.
     * @return returns the playeraction that contains the unit actions that make the units attack the nearest enemy unit.
     */
    public PlayerAction StuckGameRush()
    {
    	//go though all units stored in the physical gamestate
    	for(Unit u: StartGameState.getPhysicalGameState().getUnits())
    	{
    		//check to see If the we own the unit
    		if(u.getPlayer() == MaxPlayer && u.getType().canAttack)
    		{
    			//An inherited function that will created the required unit action that will let the current unit u attack the nearest enemy
    	        attack(u, getNearestEnemy(StartGameState.getPhysicalGameState(), StartGameState.getPlayer(MaxPlayer), u));
    		}
    	}
    	
    	//inherited function that will turn the unit actions into a player action
    	return translateActions(MaxPlayer, StartGameState);
    }
	
    /**
     * This function is taken from the sample bots. This just loops through and plays out the actions from the given time. 
     * This is quite a costly function, reducing the time will increase the amount of computations you can do but
     * will may make the evaluation worse depending on how long the unit action takes to execute
     * @param gs		  The gamestate to be progressed 
     * @param time		  How long should the gamestate be progressed
     * @throws Exception  This is bad practice. But a lot of the authors code throws exceptions rather than handling them
     */
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
	
}
