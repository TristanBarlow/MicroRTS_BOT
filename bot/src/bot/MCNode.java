package bot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

import ai.abstraction.pathfinding.GreedyPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import util.Pair;


import rts.GameState;
import rts.PhysicalGameState;
import rts.PlayerAction;
import rts.PlayerActionGenerator;
import rts.ResourceUsage;
import rts.UnitAction;
import rts.units.Unit;



/**
 * @author Tristan
 * MCNode is used to construct our gamestate tree. Creation of new MCNodes is done primarly inside the MCNode class
 * The one exception being the root node. Which is created at the start of a new computation. 
 * 
 */
public class MCNode
{

   //This is the move that was taken to reach this node.
   private PlayerAction move;
   
   //This is the parent of this current node. Null if the node is the root node
   //This is used for initialisation and back propagation. 
   private MCNode parentNode = null;
   
   //A copy of the Gamestate with the unit actions of "move" stored inside 
   private GameState gsCopy;
   
   //Integer is the hashcode (key) of the move that is stored in the child (value)
   //This is used to check to see if the move has already been used. As a class with exactly the same
   // attribute values generate the same hash.
   private LinkedHashMap<Integer, MCNode> childActionMap = null;
   
   //This idea was taken from the NaiveMCTs example. It is a list of all the current moves for each unit
   // each unit has its own action table.
   private ArrayList<MCUnitActions> unitActionTable;
   
   //Sampled moves is a list of all the moves that this node has tried. It is limited to the current node
   // And the gamestate this node represents.
   private ArrayList<PlayerAction> sampledMoves;
   
   //RushUnits is used as a list of units that are to be made to either move towards the enemy or attack the enemy if in range
   //units are placed in here during the creating of the unitActionTable, Units are appended depending pre determined rules.
   private ArrayList<Unit>RushUnits;
   
   //This is the probability out of 1 that it will choose an exisitng greedy child,seen it called epsilon blah blah.
   //Its basically exploitation vs exploration. 
   private double Exploitation = 0.4;
   
   //Used to determine in some random calculations.
   private Random r = new Random();
   
   //this is the total evaluation score that this node has accumulated for the evaluaiton of its children and it.
   private double TotalEvaluation =0;
   
   //The number of visits this node has had during the computation.
   private int Visits = 0;
   
   //After the game has cycled until a player can move again, if the game has ended this will turn true. This stops
   // some nasty exceptions when you try and do stuff with empty lists etc.
   private boolean EndGame = false;
   
   //This can be used to either generate already complete PlayerActions (ive tried dont bother thats not useful)
   //Or used to get a list of all the units and their available actions.(This is the good one).
   private PlayerActionGenerator PAG = null;
   
   //If true, it will filter out any actions the workers have that are of type produce.
   // This should be set to true on small maps, as sometimes they try and fail to build a barracks
   private boolean buildBarracks = false;
   
   //The pathfinding that is to be used to find the route to the nearest enemy(only used for the rush units)
   PathFinding pathFinding = new GreedyPathFinding();
   
   //the player the moves are currently being sampled for. (Not the owner Of the move stored in the move field!!!)
   public int player = 0;
   
   //The player we want to win
   private int maxPlayer = 0;
   
   //The player we want to lose
   private int minPlayer =0;

   //The max depth allowed for the algorithm to explore. Will return self if Maxdepth >= depth
   private int MaxDepth = 10;
   
   //The current depth of the node.
   private int Depth = 0;

   //setters getters
   public GameState GetGamestate() {return gsCopy;}
   public MCNode GetParentNode() {return parentNode;}
   public int GetNumberOfChildren() {return childActionMap.size();}
   public PlayerAction GetNodeMove(){return move;}
   
   /**
    * This constructor should only be used for the root node. 
    * There is a second constructor from the children. Some data is persitent through each node
    * that is initialised in the children by using the parents values.
    * @param MaxPlayer	   Used to set the player that we want to win.
    * @param MinPlayer	   Used to set the player that we want to lose.
    * @param gs			   For the root node. this will be a copy of the start gamestate.
    * @param MAXDEPTH	   Used to set the maxdepth of the tree.
    * @param BuildBarracks Allows or disallows the building of a barracks.
    * @throws Exception    Why, why.
    */
   public MCNode(int MaxPlayer, int MinPlayer, GameState gs, int MAXDEPTH, boolean BuildBarracks)throws Exception
   {
	   	   //Setting variables, player always equals maxplayer for the root node.
	   	   player = maxPlayer;
		   maxPlayer = MaxPlayer;
		   minPlayer = MinPlayer;
		   buildBarracks = BuildBarracks;
		   gsCopy = gs;
		   MaxDepth = MAXDEPTH;
		   
		   while(!gsCopy.canExecuteAnyAction(maxPlayer) && !gsCopy.gameover() && !gsCopy.canExecuteAnyAction(minPlayer) )
		   {
			   gsCopy.cycle();
		   }
		   if(gsCopy.gameover()&& gsCopy.winner() != -1 )
		   {
			   EndGame = true;
		   }
		   else if(gsCopy.canExecuteAnyAction(maxPlayer))
		   {
			   player = maxPlayer;
			   Init();
		   }

   }
   
   //Main COnstructor for most Nodes
   public MCNode(PlayerAction tMove, GameState gs, MCNode parent) throws Exception
   {
	   move = tMove;
	   gsCopy = gs;
	   maxPlayer = parent.maxPlayer;
	   minPlayer = parent.minPlayer;
	   buildBarracks = parent.buildBarracks;
	   Depth = parent.Depth+1;
	   parentNode = parent;
	   MaxDepth = parent.MaxDepth;
	   while(!gsCopy.canExecuteAnyAction(maxPlayer) && !gsCopy.gameover() && !gsCopy.canExecuteAnyAction(minPlayer) )
	   {
		   gsCopy.cycle();
	   }
	   if(gsCopy.gameover()&& gsCopy.winner() != -1 )
	   {
		   EndGame = true;
	   }
	   else if(gsCopy.canExecuteAnyAction(maxPlayer))
	   {
		   player = maxPlayer;
		   Init();
	   }
	   else if(gsCopy.canExecuteAnyAction(minPlayer))
	   {
		   player = minPlayer;
		   Init();
	   }

   }
   
   private void Init() throws Exception
   {
	   PAG = new PlayerActionGenerator(gsCopy, player);
	   RushUnits = new ArrayList<Unit>();
		PopulateActionTable();
	   sampledMoves = new ArrayList<PlayerAction>();
   }
   
   public MCNode GetChild(int MaxPlayer, int MinPlayer) throws Exception
   {
	   if(Depth >= MaxDepth) return this;

	   if(!EndGame && childActionMap.size() > 0  && r.nextDouble() >= Exploitation)
	   {
		   return GetGreedyChild(MaxPlayer, MinPlayer).GetChild(MaxPlayer, MinPlayer);
	   }
	   else if(!EndGame)
	   {
		   return AddActionTableNode(MaxPlayer, MinPlayer);
	   }
	   
	   return this;
   }
   
   public void PropogateValue(double Eval)
   {
	   TotalEvaluation += Eval;
       Visits++;
       if(parentNode != null)
       {
    	   parentNode.UpdateParentActionTableEntry(move, Eval);
       }
   }

   public MCNode GetBestNode()
   {
	   MCNode best =null;
	   for(MCNode mc : childActionMap.values())
	   {
		   if(best == null || mc.Visits >= best.Visits)
		   {
			   best = mc;
		   }
		   else if(mc.Visits == best.Visits && mc.TotalEvaluation > best.TotalEvaluation)
		   {
			   best = mc;
		   }
	   }
	   return best;
   }
  
   private MCNode GetGreedyChild(int MaxPlayer, int MinPlayer)   
   {
	   MCNode greediestChild = null;
	   int pSwitch = 1;
   		if(player == MinPlayer)pSwitch = -1;
       for(MCNode c: childActionMap.values())
       {
    	   double averageEvaluation = c.TotalEvaluation/c.Visits*pSwitch;
           if (greediestChild==null || averageEvaluation>(greediestChild.TotalEvaluation/greediestChild.Visits)) 
           {
               greediestChild = c;
           } 
       }
		   return greediestChild;
   }

   private void PopulateActionTable()
   {
	   	unitActionTable = new ArrayList<MCUnitActions>();
	   	childActionMap = new LinkedHashMap<Integer, MCNode>();
	    for (Pair<Unit, List<UnitAction>> choice : PAG.getChoices()) 
	    {
	    	MCUnitActions unitAction = new MCUnitActions(choice.m_a,(ArrayList<UnitAction>)choice.m_b);
	    	if((choice.m_a.getType().canHarvest || choice.m_a.getType().produces.size()>0))
	    	{
		    	
		    	unitActionTable.add(unitAction);
	    	}
	    	else
	    	{
	    		RushUnits.add(choice.m_a);
	    	}
	    }
	    
   }
   
   private MCNode AddActionTableNode(int MaxPlayer, int MinPlayer) throws Exception
   {
       for(MCUnitActions unitActions: unitActionTable)
       {
    	   boolean isMinPlayer = true;
    	   if(player == MaxPlayer) isMinPlayer = false;
           unitActions.CalculateActionWeights(isMinPlayer);  
       }
       
       
      //Get the resource usage, so we can check to see if we are creating valid actions
       ResourceUsage base_ru = GetResourceUsage();
      
       PhysicalGameState pgs = gsCopy.getPhysicalGameState();

	   PlayerAction FinalAction;
       FinalAction = new PlayerAction();
       FinalAction.setResourceUsage(base_ru.clone());
       
       for(Unit u: RushUnits)
       {
    	   Unit e = MCKarlo.getNearestEnemy(gsCopy.getPhysicalGameState(), gsCopy.getPlayer(player), u);
    	   if(e != null)
    	   {
    		   TryAndRushUnit(u, e, FinalAction, pgs);
    	   }
       }
       
       for(int i =0;  i <  unitActionTable.size(); i++)
       {
               MCUnitActions unitActions = unitActionTable.get(i);
               Unit u = unitActions.GetUnit();
               UnitAction ua;
               ResourceUsage r2 = null;
               while(true)
               {   
                   ua = unitActions.GetWeightedAction();
                   if(!buildBarracks && (u.getType().canHarvest && ua.getType() == UnitAction.TYPE_PRODUCE))
                   {
                	   //System.out.println("Trying to build a barracks when I shouldnt!! bad bot");
                   }
                   else 
                   {
	                   r2 = ua.resourceUsage(unitActions.GetUnit(), gsCopy.getPhysicalGameState());
	                   if(FinalAction.getResourceUsage().consistentWith(r2, gsCopy)) break;
                   }
               }
               FinalAction.getResourceUsage().merge(r2);
               FinalAction.addUnitAction(unitActions.GetUnit(), ua);
       }
       
       MCNode node = childActionMap.get(FinalAction.hashCode());
       if(node == null)
       {
    	  return AddChild(MaxPlayer, MinPlayer, FinalAction);
       }
       return node.GetChild(MaxPlayer, MinPlayer);
   }
   
   private void UpdateParentActionTableEntry(PlayerAction pa, double Eval) 
   {

       for (Pair<Unit, UnitAction> ua : pa.getActions()) 
       {
    	   MCUnitActions UnitTable = null;
           for(MCUnitActions UAs: unitActionTable) 
           {
               if (UAs.GetUnit() == ua.m_a)UnitTable = UAs;
           }
           if(UnitTable == null)
           {
        	   //this  happens when a rush unit is cycled through. 
           }
           else 
           {
        	 UnitTable.UpdateAction(ua.m_b, Eval);
           }
       }
   }
    
   private MCNode AddChild(int MaxPlayer,int MinPlayer, PlayerAction move) throws Exception
   {
	   if(move != null)
	   {
		   GameState gs = gsCopy.cloneIssue(move);
		   MCNode n = new MCNode(move, gs.clone(), this );
    	   childActionMap.put(move.hashCode(), n);
    	   sampledMoves.add(move);
		   return n;
	   }
	   return null;

   }
   
   private double GetAverageEvaluation()
   {
	   return TotalEvaluation/Visits;
   }
   
   private MCNode GetRandomChild()
   {
	  PlayerAction move = sampledMoves.get(r.nextInt(sampledMoves.size()));
	  return childActionMap.get(move.hashCode());
   }
   
   private ResourceUsage GetResourceUsage()
   {
	   ResourceUsage r = new ResourceUsage();
       for(Unit u:gsCopy.getUnits()) 
       {
           UnitAction ua = gsCopy.getUnitAction(u);
           if (ua!=null) 
           {
               ResourceUsage ru = ua.resourceUsage(u, gsCopy.getPhysicalGameState());
               r.merge(ru);
           }
       }
       return r;
   }
   
   private void TryAndRushUnit(Unit u, Unit e, PlayerAction FinalAction, PhysicalGameState pgs)
   {
	   UnitAction ua = null;
	   ResourceUsage r2 = null;
       int dx = e.getX()-u.getX();
       int dy = e.getY()-u.getY();
       double d = Math.sqrt(dx*dx+dy*dy);
       if (d<=u.getAttackRange()) 
       {
           ua = new UnitAction(UnitAction.TYPE_ATTACK_LOCATION,e.getX(),e.getY());
           if(ua != null)
           {
        	   r2 = ua.resourceUsage(u, pgs);
           }
       } 
       else
       {
    	    ua = pathFinding.findPath(u, e.getPosition(pgs), gsCopy, FinalAction.getResourceUsage());
    	    if(ua!= null)
    	    {
    	    	r2 = ua.resourceUsage(u, pgs);
    	    }
       }
	   if(r2 != null && FinalAction.getResourceUsage().consistentWith(r2, gsCopy))
	   {
		   FinalAction.getResourceUsage().merge(r2);
           FinalAction.addUnitAction(u, ua);
	   }
   }
   
}
