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
   private double Exploitation = 0.3;
   
   //Used to determine in some random calculations.
   private Random r = new Random();
   
   //this is the total evaluation score that this node has accumulated for the evaluaiton of its children and it.
   private double TotalEvaluation =0;
   
   //The number of visits this node has had during the computation.
   private int Visits = 0;
   
   //After the game has cycled until a player can move again, if the game has ended this will turn true. This stops
   // some nasty exceptions when you try and do stuff with empty lists etc.
   private boolean endGame = false;
   
   //This can be used to either generate already complete PlayerActions (ive tried dont bother thats not useful)
   //Or used to get a list of all the units and their available actions.(This is the good one).
   private PlayerActionGenerator actionGenerator = null;
   
   //If true, it will filter out any actions the workers have that are of type produce.
   // This should be set to true on small maps, as sometimes they try and fail to build a barracks
   private boolean buildBarracks = false;
   
   //The path finding that is to be used to find the route to the nearest enemy(only used for the rush units)
   PathFinding pathFinding = new GreedyPathFinding();
   
   //the player the moves are currently being sampled for. (Not the owner Of the move stored in the move field!!!)
   public int player = 0;
   
   //The player we want to win
   private int maxPlayer = 0;
   
   //The player we want to lose
   private int minPlayer =0;

   //The max depth allowed for the algorithm to explore. Will return self if Maxdepth >= depth
   private int maxDepth = 10;
   
   //The current depth of the node.
   private int depth = 0;

   //setters getters
   public GameState GetGamestate() {return gsCopy;}
   public MCNode GetParentNode() {return parentNode;}
   public int GetNumberOfChildren() {return childActionMap.size();}
   public PlayerAction GetNodeMove(){return move;}
   public int GetDepth()  {return depth;}
   
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
		   maxPlayer = MaxPlayer;
		   minPlayer = MinPlayer;
	   	   player = maxPlayer;
		   buildBarracks = BuildBarracks;
		   gsCopy = gs;
		   maxDepth = MAXDEPTH;
		   
		   //Cycle the gamestate until a point where any player can make a move.
		   CycleGameState();
		   
		   //Since it is the root, we know the next player to make a move is the maxplayer
		   //but just a sanity check anyway. Might catch a random error
		   if(gsCopy.canExecuteAnyAction(maxPlayer))
		   {
			   player = maxPlayer;
			   //initialise the node variables
			   Init();
		   }

   }
   

/**
 * This is the main constructor for most nodes.
 * @param tMove       this is the last move to be made that resulted in this gamestate
 * @param gs          this is the gamesate after the move tmove has been issued to it.
 * @param parent      the node that is responsible for the creation of the current node.
 * @throws Exception  blah blah same old bad practice.(not me)
 */
   public MCNode(PlayerAction tMove, GameState gs, MCNode parent) throws Exception
   {
	   //set the move(tMove) that got us to the state (gs) gs has the move (tmove)
	   //already set to execute, the game needs to cycle to carryout the move.
	   move = tMove;
	   gsCopy = gs;
	   
	   //get the data from the parent node.
	   maxPlayer = parent.maxPlayer;
	   minPlayer = parent.minPlayer;
	   buildBarracks = parent.buildBarracks;
	   parentNode = parent;
	   maxDepth = parent.maxDepth;
	   
	   //increment depth
	   depth = parent.depth+1;
	   
	   //cycle the gamestate until a player can move... or not
	   CycleGameState();
	   
	   if(!endGame)
	   {
		   if(gsCopy.canExecuteAnyAction(maxPlayer))
		   {
			   //if the next player that can move is the maxplayer
			   //we know that the move(tmove) was the minplayers move.
			   player = maxPlayer;
		   }
		   else  if(gsCopy.canExecuteAnyAction(minPlayer))
		   {
			   //if the next player that can move is the minaplayer
			   //we know that the move(tmove) was the maxplayers move.
			   player = minPlayer;
	
		   }
		   Init();
	   }
   }
   
   /**
    * Responsible for initialising certain variables. Also where the unit action tables
    * calls are done
    * @throws Exception
    */
   private void Init() throws Exception
   {
	   // create new classes for the member variables
	   actionGenerator = new PlayerActionGenerator(gsCopy, player);
	   RushUnits = new ArrayList<Unit>();
	   sampledMoves = new ArrayList<PlayerAction>();
	   unitActionTable = new ArrayList<MCUnitActions>();
	   childActionMap = new LinkedHashMap<Integer, MCNode>();
	  
	   //Call that will use the action generator to populate the unitActionTable
	   PopulateActionTable();
   }
   
   /**
    * This function will return a child of the node it is called from
    * it may go several layers deep node to node, before it returns a child
    * It will either create a new child and return, or use a greedy search 
    * to return the best current child node.
    * @return Returns the reference to a child either new or old or this, depending on the node
    * @throws Exception
    */
   public MCNode GetChild() throws Exception
   {
	   
	   //depth check, if max depth return the current node.
	   if(depth >= maxDepth) return this;

	   //If it has reached the endgame state return
	   if(endGame)return this;
	   
	   //If it isnt an endgame state, and we have children to look through, leave it to random
	   //chance to decide whether or not to get a greedy child or add a new child
	   // a 60% chance to exploit seems a good number. Changing it depending
	   //on the evaluation of the node might be a good idea. I will test
	   if(childActionMap.size() > 0  && r.nextDouble() <= Exploitation)
	   {
		   return GetGreedyChild().GetChild();
	   }
	   
	   //if its not end game and the rng decided we're going to explore
	   //return a new node(maybe) based on the action table stored in the unitActionTable
	   else
	   {
		   return AddActionTableNode();
	   }
   }
   
   /**
    * this function propagates the evaluation of the gamestate.
    * In addition it will increment the vists to this node.
    * And will call to update the Moves action table entry
    * in the parent node to this node.
    * @param Eval The evaluation of the gamestate to be propagate
    */
   public void PropogateValue(double Eval)
   {
	   // increment visits, add Eval to total evaluation
	   TotalEvaluation += Eval;
       Visits++;
       
       //this will only fail if its the root node
       if(parentNode != null)
       {
    	   //Update the value of the move(move)so that any unit actions
    	   //used will receive the Eval to determine if the unit action was good or not
    	   parentNode.UpdateParentActionTableEntry(move, Eval);
       }
   }

   /**
    * This function sorts through the current childaction map of the node
    * it will return the node with the most visits, if two nodes have the same
    * amount of visits it will return the one with the best evaluation.
    * @return A refernce to the best node. 
    */
   public MCNode GetBestNode()
   {
	   //empty best node.
	   MCNode best =null;
	   
	   //for loop going through all the values in childactionMap
	   //evaluated as shown.
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
  
   /**
    * When an action is issued with "clone issue" you get a copy of the gamestate
    * with the actions "loaded" into it. This function will play out the action
    * And break when it finds when a player can move. It also checks for an endgame
    * state.
    */
   private void CycleGameState()
   {	
	   //cycle game until someone can use a move, or the game is over.
	   while(!gsCopy.canExecuteAnyAction(maxPlayer) && !gsCopy.gameover() && !gsCopy.canExecuteAnyAction(minPlayer) )
		{
			   gsCopy.cycle();
		}
	   
	   //if the gameover caused the loop to break, set this node as a terminal state node.
	   if(gsCopy.gameover()&& gsCopy.winner() != -1 )
	   {
		   endGame = true;
		   
	   }
   }
   
   /**
    * This function sorts cycles through all the children of the current node, and gets child with the largest average evaluation
    * @return A reference to the child with the highest average evaluation.
    */
   private MCNode GetGreedyChild()   
   {
	   //This switches depending on the player, the minplayer nodes store negative values
	   //so we multiply by the switch to change it to positive to work for the comparison
	   int pSwitch = 1;
   	   if(player == minPlayer)pSwitch = -1;
   	   
   	   //Node that will be returned
	   MCNode greediestChild = null;
	   
	   //loop through all the values in the child action map and compare as shown below.
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

   /**
    * This function, will use the action generator to get all the units and all their choices
    */
   private void PopulateActionTable()
   {
	    for (Pair<Unit, List<UnitAction>> choice : actionGenerator.getChoices()) 
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
   
   /**
    * This is the main function that handles the creation of new player actions from the list of units and there available unit actions
    * Some of the code, especially to do with the merging resource usages etc. Has been adapted from the sample bots.
    * @return            It will return either a new node with its generated player action. Or an existing node with the
    *                    same action that was generated
    * @throws Exception
    */
   private MCNode AddActionTableNode() throws Exception
   {
	   
	   //Calculate each actions value for each unit in the action table
       for(MCUnitActions unitActions: unitActionTable)
       {
    	   //used to as a switch, as the min player evaluaitons are negative. but we still
    	   //want to assume the minplayer will pick the best moves
    	   boolean isMinPlayer = true;
    	   if(player == maxPlayer) isMinPlayer = false;
    	   
    	   //call the function that will do the calculations.
           unitActions.CalculateActionWeights(isMinPlayer);  
       }
       
       
      //Get the resource usage, so we can check to see if we are creating valid actions
       ResourceUsage base_ru = GetResourceUsage();
      
       //get the physical gamestate, assigned to a local variable to reduce the amount of 
       // function calls.
       PhysicalGameState pgs = gsCopy.getPhysicalGameState();

       //create the player action we will fill with unit actions
	   PlayerAction FinalAction;
       FinalAction = new PlayerAction();
       
       //give the player action the resource usage it is allowed for its move
       FinalAction.setResourceUsage(base_ru.clone());
       
       //Make any Rush units rush.
       for(Unit u: RushUnits)
       {
    	   Unit e = MCKarlo.getNearestEnemy(gsCopy.getPhysicalGameState(), gsCopy.getPlayer(player), u);
    	   
    	   //quick check to see if there are any enemies left to attack
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
    	  return AddChild(FinalAction);
       }
       return node.GetChild();
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
    
   private MCNode AddChild(PlayerAction move) throws Exception
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
