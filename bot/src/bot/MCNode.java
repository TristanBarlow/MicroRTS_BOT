package bot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import util.Pair;


import rts.GameState;
import rts.PlayerAction;
import rts.PlayerActionGenerator;
import rts.ResourceUsage;
import rts.UnitAction;
import rts.units.Unit;



public class MCNode
{
   public PlayerAction Move;
   public MCNode ParentNode = null;
   public GameState GSCopy;
   public LinkedHashMap<Integer, MCNode> ChildActionMap = null;
   public ArrayList<MCUnitActions> UnitActionTable;
   public ArrayList<PlayerAction> SampledMoves;
   private double Exploitation = 0.4;
   private double EpsilonG = 0;
   private Random r = new Random();
   public double TotalEvaluation =0;
   public double Evaluation = 0;
   public int Visits = 0;
   public boolean EndGame = false;
   public PlayerActionGenerator PAG = null;
   public boolean HasMoreAction = false;
   public int Player = 0;
   
   public ArrayList<MCActionValue> HighPotenialMoves;

   public double CheapEvaluation = 0.2;
   boolean CanBuildBarracks = false;
   private int MaxSampledMoves = 100;
   private int MaxRandomMoves = 50;
   private int MaxSampleIterations = 5000;
   private boolean ShouldSampleMoves = true;
   
   private static double AttackValue = 2;
   private static double HarvestValue = 5;
   private static double ReturnValue = 1;
   private static double ProduceValue = 1;
   
   
   public int MaxDepth = 10;
   public int Depth = 0;
   
   public int MaxBreadth =0;
   public int Breadth = 0;
   

   public long CutOffTime = 0;

 
   
   //RootNode only
   public MCNode(int MaxPlayer, int MinPlayer, GameState gs, int MAXDEPTH, int breadth, long cutOffTime, int IsMaxBiasSamples, boolean bigMap)throws Exception
   {
	   	if(Player == 1)MinPlayer = 0;
		   GSCopy = gs;
		   MaxDepth = MAXDEPTH;
		   MaxBreadth = breadth;
		   CanBuildBarracks = bigMap;
		   CutOffTime = cutOffTime;
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
   public MCNode(int MaxPlayer, int MinPlayer, PlayerAction move, GameState gs, MCNode parent) throws Exception
   {
	   	if(Player == 1)MinPlayer = 0;
	   Move = move;
	   Depth = parent.Depth+1;
	   ParentNode = parent;
	   MaxDepth = parent.MaxDepth;
	   MaxBreadth = parent.MaxBreadth;
	   CanBuildBarracks = parent.CanBuildBarracks;
	   GSCopy = gs;
	   CutOffTime = parent.CutOffTime;
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
		PopulateActionTable();
//	   PAG.randomizeOrder();
	   HighPotenialMoves = new ArrayList<MCActionValue>();
	   CheapEvaluation = GetCheapEvaluationBound(CheapEvaluation);
	   SampledMoves = new ArrayList<PlayerAction>();
	   HasMoreAction = true;
   }
   
   private double GetCheapEvaluationBound(double Bound)
   {
	   double total = AttackValue + ProduceValue + ReturnValue + HarvestValue;
	   total = total/4;
	   return total*Bound;
   }
   
   public double GetAverageEvaluation()
   {
	   return TotalEvaluation/Visits;
   }
   
   public MCNode GetChild(int MaxPlayer, int MinPlayer) throws Exception
   {
	   if(Depth >= MaxDepth) return this;

	   if(!EndGame && ChildActionMap.size() > 0  && r.nextDouble() <= Exploitation)
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
	   MCNode GreedyChild = null;
	   
	   if(r.nextFloat() >= EpsilonG) 
	   {
	           for(MCNode c: ChildActionMap.values())
	           {
	        	   if(Player == MaxPlayer)
	        	   {
	                   if (GreedyChild==null || (c.TotalEvaluation/c.Visits)>(GreedyChild.TotalEvaluation/GreedyChild.Visits)) 
	                   {
	                       GreedyChild = c;
	                   } 
	        	   }
	        	   else 
	        	   {
	                   if (GreedyChild==null || (c.TotalEvaluation/c.Visits)<(GreedyChild.TotalEvaluation/GreedyChild.Visits)) 
	                   {
	                       GreedyChild = c;
	                   }
	        	   }
	           }
	    	   return GreedyChild;
       } 
	   else 
	   {
           // choose one at random from the ones seen so far:
           return GetRandomChild();
       }
   }
   
   public MCNode AddRandomChild(int MaxPlayer, int MinPlayer) throws Exception
   {
	   PlayerAction FinalAction = GetMove();
	   GameState gs = GSCopy.cloneIssue(FinalAction);	
	   MCNode n = new MCNode(MaxPlayer,MinPlayer, FinalAction, gs.clone(), this );
	   SampledMoves.add(FinalAction);
	   ChildActionMap.put(FinalAction.hashCode(), n);
	   return n;
   }
   
   public MCNode GetRandomChild()
   {
	  PlayerAction move = SampledMoves.get(r.nextInt(SampledMoves.size()));
	  return ChildActionMap.get(move.hashCode());
   }
   
   public PlayerAction GetMove() throws Exception
   {
	   if(HighPotenialMoves.size() > 0)
	   {
	   		return HighPotenialMoves.remove(0).GetPlayerAction();
	   }
	   
	   if(SampledMoves.size() > 0) return SampledMoves.remove(r.nextInt(SampledMoves.size()));
	   
	   if(HasMoreAction)
	   {
		   	PlayerAction pa =  PAG.getNextAction(CutOffTime);
		   	if(pa != null)
		   	{
		   		return pa;
		   	}
		   	HasMoreAction = false;
	   }
	   
	   return new PlayerAction();

   }
   
   private void SampleAvailableMoves() throws Exception
   {
	   int iter =0;
	   while(HighPotenialMoves.size() < MaxSampledMoves && iter < MaxSampleIterations )
	   {
		   PlayerAction move = PAG.getNextAction(CutOffTime);
		   if(move != null)
		   {
			   EvaluateMove(move);
		   }
		   else
		   {
			   HasMoreAction = false;
			   break;
		   }
		   iter++;
	   }
	   ShouldSampleMoves = false;
	   HighPotenialMoves.sort((m2, m1) -> Double.compare(m1.GetValue(), m2.GetValue()));
   }
   
   public MCNode AddQuickBiasChild(int MaxPlayer,int MinPlayer) throws Exception
   {
	   if(ShouldSampleMoves)
	   {
		   SampleAvailableMoves();
	   }
	   return AddChild(MaxPlayer, MinPlayer, GetMove());
   }
   
   
  public MCNode AddChild(int MaxPlayer,int MinPlayer, PlayerAction move) throws Exception
   {
	   if(move != null)
	   {
		   GameState gs = GSCopy.cloneIssue(move);
		   MCNode n = new MCNode(MaxPlayer,MinPlayer, move, gs.clone(), this );
    	   ChildActionMap.put(move.hashCode(), n);
    	   SampledMoves.add(move);
		   Breadth++;
		   return n;
	   }
	   return null;

   }
  
   public double EvaluateMove(PlayerAction move)
   {
	   double h = 0;
	   double a = 0;
	   double r = 0;
	   double p = 0;
	   int size = 0;
	   boolean ShouldAdd = true;
	   for(Pair<Unit, UnitAction> UnitPair : move.getActions())
	   {
		   size++;
		   if(UnitPair.m_b.getType() == UnitAction.TYPE_ATTACK_LOCATION)
		   {
			   a += 1*AttackValue;
		   }
		   else if(UnitPair.m_b.getType() == UnitAction.TYPE_HARVEST)
		   {
			   h += 1*HarvestValue;
		   }
		   else if(UnitPair.m_b.getType() == UnitAction.TYPE_PRODUCE)
		   {
				  if(UnitPair.m_a.getType().canHarvest && !CanBuildBarracks )
				  {
					  p = -100000;
					 ShouldAdd = false;
				  }
			   p += 1*ProduceValue;
		   }
		   else if(UnitPair.m_b.getType() == UnitAction.TYPE_RETURN)
		   {
			   r += 1*ReturnValue;
		   }
	   }
	   double result = a+h+r+p/size;
	   if(result > CheapEvaluation && HighPotenialMoves.size() < MaxSampledMoves && ShouldAdd)
	   {
	   		HighPotenialMoves.add(new MCActionValue(move, result));
	   		return result;
	   }
	   else if(SampledMoves.size() < MaxRandomMoves  && ShouldAdd)
	   {
		   SampledMoves.add(move);
		   return result;
	   }
	   return result;
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
	   	int idx = 0;
	    for (Pair<Unit, List<UnitAction>> choice : PAG.getChoices()) 
	    {
	    	MCUnitActions unitAction = new MCUnitActions(choice.m_a,(ArrayList<UnitAction>)choice.m_b);
	    	UnitActionTable.add(unitAction);
            idx++;
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
       
	   PlayerAction FinalAction;
       FinalAction = new PlayerAction();
       FinalAction.setResourceUsage(base_ru.clone());
       for(int i =0;  i <  UnitActionTable.size(); i++)
       {
               MCUnitActions unitActions = UnitActionTable.get(i);
               UnitAction ua;
               ResourceUsage r2 = null;
               while(true)
               {   
                   ua = unitActions.GetWeightedAction();
                   
                   if(ua == null)break;
                   
                   r2 = ua.resourceUsage(unitActions.GetUnit(), GSCopy.getPhysicalGameState());                            
            	   if(FinalAction.getResourceUsage().consistentWith(r2, GSCopy)) break;
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
           if(UnitTable == null) System.out.println("UnitTable not found");
           UnitTable.UpdateAction(ua.m_b, Eval);
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
}
