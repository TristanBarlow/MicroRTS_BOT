package bot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

import ai.abstraction.Attack;
import ai.abstraction.pathfinding.GreedyPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.montecarlo.MonteCarlo.PlayerActionTableEntry;
import util.Pair;


import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.PlayerActionGenerator;
import rts.ResourceUsage;
import rts.UnitAction;
import rts.units.Unit;



public class MCNode
{

   //This is the move that was taken to reach this node. If the 
   private PlayerAction Move;
   public MCNode ParentNode = null;
   public GameState GSCopy;
   public LinkedHashMap<Integer, MCNode> ChildActionMap = null;
   public ArrayList<MCUnitActions> UnitActionTable;
   public ArrayList<PlayerAction> SampledMoves;
   public ArrayList<Unit>RushUnits;
   private double Exploitation = 0.4;
   private double EpsilonG = 0;
   private Random r = new Random();
   public double TotalEvaluation =0;
   public double Evaluation = 0;
   public int Visits = 0;
   public boolean EndGame = false;
   public PlayerActionGenerator PAG = null;
   public boolean HasMoreAction = false;
   private boolean buildBarracks = false;
   
   PathFinding pathFinding = new GreedyPathFinding();
   
   public int Player = 0;
   private int MaxPlayer = 0;
   private int MinPlayer =0;

   public int MaxDepth = 10;
   public int Depth = 0;
   private int Breadth = 0;

   //RootNode only
   public MCNode(int maxPlayer, int minPlayer, GameState gs, int MAXDEPTH, boolean BuildBarracks)throws Exception
   {
	   	   Player = maxPlayer;
		   MaxPlayer = maxPlayer;
		   MinPlayer = minPlayer;
		   buildBarracks = BuildBarracks;
		   GSCopy = gs;
		   MaxDepth = MAXDEPTH;
		   while(!GSCopy.canExecuteAnyAction(MaxPlayer) && !GSCopy.gameover() && !GSCopy.canExecuteAnyAction(MinPlayer) )
		   {
			   GSCopy.cycle();
		   }
		   if(GSCopy.gameover()&& GSCopy.winner() != -1 )
		   {
			   EndGame = true;
		   }
		   else if(GSCopy.canExecuteAnyAction(MaxPlayer))
		   {
			   Player = MaxPlayer;
			   Init();
		   }

   }
   
   
   //Main COnstructor for most Nodes
   public MCNode(PlayerAction move, GameState gs, MCNode parent) throws Exception
   {
	   Move = move;
	   GSCopy = gs;
	   MaxPlayer = parent.MaxPlayer;
	   MinPlayer = parent.MinPlayer;
	   buildBarracks = parent.buildBarracks;
	   Depth = parent.Depth+1;
	   ParentNode = parent;
	   MaxDepth = parent.MaxDepth;
	   while(!GSCopy.canExecuteAnyAction(MaxPlayer) && !GSCopy.gameover() && !GSCopy.canExecuteAnyAction(MinPlayer) )
	   {
		   GSCopy.cycle();
	   }
	   if(GSCopy.gameover()&& GSCopy.winner() != -1 )
	   {
		   EndGame = true;
	   }
	   else if(GSCopy.canExecuteAnyAction(MaxPlayer))
	   {
		   Player = MaxPlayer;
		   Init();
	   }
	   else if(GSCopy.canExecuteAnyAction(MinPlayer))
	   {
		   Player = MinPlayer;
		   Init();
	   }

   }
   
   private void Init() throws Exception
   {
	   PAG = new PlayerActionGenerator(GSCopy, Player);
	   RushUnits = new ArrayList<Unit>();
		PopulateActionTable();
	   SampledMoves = new ArrayList<PlayerAction>();
	   HasMoreAction = true;
   }
   
   public double GetAverageEvaluation()
   {
	   return TotalEvaluation/Visits;
   }
   
   public MCNode GetChild(int MaxPlayer, int MinPlayer) throws Exception
   {
	   if(Depth >= MaxDepth) return this;

	   if(!EndGame && ChildActionMap.size() > 0  && r.nextDouble() >= Exploitation)
	   {
		   return GetGreedyChild(MaxPlayer, MinPlayer).GetChild(MaxPlayer, MinPlayer);
	   }
	   else if(!EndGame)
	   {
		   return AddActionTableNode(MaxPlayer, MinPlayer);
	   }
	   
	   return this;
   }
   
   public MCNode GetGreedyChild(int MaxPlayer, int MinPlayer)   
   {
	   MCNode greediestChild = null;
	   int pSwitch = 1;
	   if(Player == MinPlayer)pSwitch = -1;
	   
	   if(r.nextFloat() >= EpsilonG) 
	   {
	       for(MCNode c: ChildActionMap.values())
	       {
	    	   double averageEvaluation = c.TotalEvaluation/c.Visits*pSwitch;
	           if (greediestChild==null || averageEvaluation>(greediestChild.TotalEvaluation/greediestChild.Visits)) 
	           {
	               greediestChild = c;
	           } 
	       }
		   return greediestChild;
	   }
	   else 
	   {
           // choose one at random from the ones seen so far:
           return GetRandomChild();
       }
   }
   
   public MCNode GetRandomChild()
   {
	  PlayerAction move = SampledMoves.get(r.nextInt(SampledMoves.size()));
	  return ChildActionMap.get(move.hashCode());
   }

   public MCNode AddChild(int MaxPlayer,int MinPlayer, PlayerAction move) throws Exception
   {
	   if(move != null)
	   {
		   GameState gs = GSCopy.cloneIssue(move);
		   MCNode n = new MCNode(move, gs.clone(), this );
    	   ChildActionMap.put(move.hashCode(), n);
    	   SampledMoves.add(move);
		   Breadth++;
		   return n;
	   }
	   return null;

   }
  
   public MCNode GetBestNode()
   {
	   MCNode best =null;
	   for(MCNode mc : ChildActionMap.values())
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
   
   public void PopulateActionTable()
   {
	   	UnitActionTable = new ArrayList<MCUnitActions>();
	   	ChildActionMap = new LinkedHashMap<Integer, MCNode>();
	    for (Pair<Unit, List<UnitAction>> choice : PAG.getChoices()) 
	    {
	    	MCUnitActions unitAction = new MCUnitActions(choice.m_a,(ArrayList<UnitAction>)choice.m_b);
	    	if((choice.m_a.getType().canHarvest || choice.m_a.getType().produces.size()>0))
	    	{
		    	
		    	UnitActionTable.add(unitAction);
	    	}
	    	else
	    	{
	    		RushUnits.add(choice.m_a);
	    	}
	    }
	    
   }
   
   public MCNode AddActionTableNode(int MaxPlayer, int MinPlayer) throws Exception
   {
       for(MCUnitActions unitActions: UnitActionTable)
       {
    	   boolean isMinPlayer = true;
    	   if(Player == MaxPlayer) isMinPlayer = false;
           unitActions.CalculateActionWeights(isMinPlayer);  
       }
       
       
      //Get the resource usage, so we can check to see if we are creating valid actions
       ResourceUsage base_ru = GetResourceUsage();
      
       PhysicalGameState pgs = GSCopy.getPhysicalGameState();

	   PlayerAction FinalAction;
       FinalAction = new PlayerAction();
       FinalAction.setResourceUsage(base_ru.clone());
       
       for(Unit u: RushUnits)
       {
    	   Unit e = MCKarlo.getNearestEnemy(GSCopy.getPhysicalGameState(), GSCopy.getPlayer(Player), u);
    	   if(e != null)
    	   {
    		   TryAndRushUnit(u, e, FinalAction, pgs);
    	   }
       }
       
       for(int i =0;  i <  UnitActionTable.size(); i++)
       {
               MCUnitActions unitActions = UnitActionTable.get(i);
               Unit u = unitActions.GetUnit();
               UnitAction ua;
               ResourceUsage r2 = null;
               while(true)
               {   
                   ua = unitActions.GetWeightedAction();
                   if(!buildBarracks && (u.getType().canHarvest && ua.getType() == UnitAction.TYPE_PRODUCE))
                   {
                	   System.out.println("Trying to build a barracks when I shouldnt!! bad bot");
                   }
                   else 
                   {
	                   r2 = ua.resourceUsage(unitActions.GetUnit(), GSCopy.getPhysicalGameState());
	                   if(FinalAction.getResourceUsage().consistentWith(r2, GSCopy)) break;
                   }
               }
               FinalAction.getResourceUsage().merge(r2);
               FinalAction.addUnitAction(unitActions.GetUnit(), ua);
       }
       
       MCNode node = ChildActionMap.get(FinalAction.hashCode());
       if(node == null)
       {
    	  return AddChild(MaxPlayer, MinPlayer, FinalAction);
       }
       return node.GetChild(MaxPlayer, MinPlayer);
   }
   
   public void UpdateParentActionTableEntry(PlayerAction pa, double Eval) 
   {

       for (Pair<Unit, UnitAction> ua : pa.getActions()) 
       {
    	   MCUnitActions UnitTable = null;
           for(MCUnitActions UAs: UnitActionTable) 
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
   
   public void PropogateValue(double Eval)
   {
	   TotalEvaluation += Eval;
       Visits++;
       if(ParentNode != null)
       {
    	   ParentNode.UpdateParentActionTableEntry(Move, Eval);
       }
   }

   public PlayerAction GetNodeMove()
   {
	   return Move;
   }
   
   private ResourceUsage GetResourceUsage()
   {
	   ResourceUsage r = new ResourceUsage();
       for(Unit u:GSCopy.getUnits()) 
       {
           UnitAction ua = GSCopy.getUnitAction(u);
           if (ua!=null) 
           {
               ResourceUsage ru = ua.resourceUsage(u, GSCopy.getPhysicalGameState());
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
    	    ua = pathFinding.findPath(u, e.getPosition(pgs), GSCopy, FinalAction.getResourceUsage());
    	    if(ua!= null)
    	    {
    	    	r2 = ua.resourceUsage(u, pgs);
    	    }
       }
	   if(r2 != null && FinalAction.getResourceUsage().consistentWith(r2, GSCopy))
	   {
		   FinalAction.getResourceUsage().merge(r2);
           FinalAction.addUnitAction(u, ua);
	   }
   }
   
}
