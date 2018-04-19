package bot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.crypto.dsig.keyinfo.KeyValue;

import rts.GameState;
import rts.PlayerAction;
import rts.PlayerActionGenerator;
import rts.UnitAction;
import rts.units.Unit;
import util.Pair;


public class MCNode
{
   public PlayerAction Move;
   public MCNode ParentNode;
   public GameState GSCopy;
   
   public ArrayList<MCNode>ChildNodes;
   
   public ArrayList<MCNode>MaybeList;
   
   public ArrayList<PlayerAction> TriedMoves;
   
   public double CheapEvaluation = -1.0;
   public PlayerAction CheapAction;
   public int MaxCheapRuns =2000;
   
   private static double C = 0.05;
   private static double AttackValue = 1.4;
   private static double HarvestValue = 1.8;
   private static double ReturnValue = 2.0;
   private static double ProduceValue = 1.4;
   
   private Random r = new Random();
 
   public double TotalEvaluation =0;
   public double Evaluation = 0;
   public int Visits = 0;
   
   public int MaxDepth = 10;
   public int Depth = 1;
   
   public int NumberOfUnits = 0;
   
   public int MaxBreadth =0;
   public int Breadth = 0;
   
   public boolean ShouldSimulate = false;
   
   public PlayerActionGenerator PAG = null;
   public boolean HasMoreAction = false;
   
   public int Player = 0;
   public int WinningPlayer = -1;
   public long CutOffTime = 0;

   public boolean EndGame = false;
   
   
   //RootNode only
   public MCNode(int MaxPlayer, int MinPlayer, GameState gs, int MAXDEPTH, int breadth, long cutOffTime)throws Exception
   {
		   Player = MaxPlayer;
		   GSCopy = gs;
		   MaxDepth = MAXDEPTH;
		   MaxBreadth = breadth;
		   CutOffTime = cutOffTime;
		   while(!gs.canExecuteAnyAction(Player) && !GSCopy.gameover() )
		   {
			   gs.cycle();
		   }
		   if(!gs.gameover()&& gs.winner() == -1 )
		   {
			   PopulateUntriedMoves();
		   }
		   if(gs.winner() == MaxPlayer)
		   {
			   PopulateUntriedMoves();
		   }
		   ChildNodes = new ArrayList<MCNode>();
		   TriedMoves = new ArrayList<PlayerAction>();

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
		   PopulateUntriedMoves();
	   }
	   else if(GSCopy.canExecuteAnyAction(MinPlayer))
	   {
		   Player = MinPlayer;
		   PopulateUntriedMoves();
	   }
	   ChildNodes = new ArrayList<MCNode>();
	   TriedMoves = new ArrayList<PlayerAction>();

   }
   
   private void PopulateUntriedMoves() throws Exception
   {
	   
	   PAG = new PlayerActionGenerator(GSCopy, Player);
	   PAG.randomizeOrder();
	   HasMoreAction = true;
   }
   
   public double GetAverageEvaluation()
   {
	   return TotalEvaluation/Visits;
   }
   
   public MCNode GetChild(int MaxPlayer, int MinPlayer) throws Exception
   {
	   if(Depth >= MaxDepth) return this;
	   
	   if((ChildNodes.size() < MaxBreadth) &&HasMoreAction && r.nextDouble() > 0.4)
	   {
		   MCNode c = AddBiasChild(MaxPlayer, MinPlayer);
		   return c;
	   }
	   
	   else if(!EndGame && ChildNodes.size() > 0)
	   {
		   ChildNodes.sort((m2, m1) -> Double.compare(m1.GetSortValue(this), m2.GetSortValue(this)));
		   MCNode c = ChildNodes.get(0);
		   return c.GetChild(MaxPlayer, MinPlayer);
	   }
	   return AddRandomChild(MaxPlayer, MinPlayer);
   }
   
   public MCNode EpislonChildGet(int MaxPlayer, int MinPlayer, float E0, float eg ) throws Exception
   {
	   if(ChildNodes.size() > 0 && r.nextFloat() <= E0+Evaluation )
	   {
		   return GetGreedyChild(eg, MaxPlayer, MinPlayer).EpislonChildGet(MaxPlayer, MinPlayer, E0, eg);
	   }
	   else
	   {
		   return GetChild(MaxPlayer, MinPlayer);
	   }
   }
   
   public MCNode GetGreedyChild(double EG, int MinPlayer, int MaxPlayer)   
   {
	   MCNode GreedyChild = null;
	   
	   if(r.nextFloat() >= EG) 
	   {
	           for(MCNode c: ChildNodes)
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
           return this.ChildNodes.get(r.nextInt(ChildNodes.size()));
       }
   }
   
   public MCNode AddRandomChild(int MaxPlayer, int MinPlayer) throws Exception
   {
	   PlayerAction FinalAction = null;
	   if(CheapAction == null)
	   {
		   if(TriedMoves.size()<1) return this;
	      FinalAction = TriedMoves.remove(r.nextInt(TriedMoves.size()));
	   }
	   GameState gs = GSCopy.cloneIssue(FinalAction);	
	   MCNode n = new MCNode(MaxPlayer,MinPlayer, FinalAction, gs.clone(), this );
	   ChildNodes.add(n);
	   return n;
   }
   
   public MCNode AddBiasChild(int MaxPlayer,int MinPlayer) throws Exception
   {
	   PAG.randomizeOrder();
	   CheapAction = null;
	   CheapEvaluation = -0.1;
	   int iter = 0;
	   while(true)
	   {
		   iter++;
		   
		   if(iter > MaxCheapRuns && CheapAction != null)
		   {
			   GameState gs = GSCopy.cloneIssue(CheapAction);	
			   MCNode n = new MCNode(MaxPlayer,MinPlayer, CheapAction, gs.clone(), this );
			   ChildNodes.add(n);
			   return n;
		   }
		   
		   PlayerAction move = GetRandomAction();
		   if(move != null)
		   {
			   GameState gs = GSCopy.cloneIssue(move);
			   EvaluateMove(gs, move);
		   }
		   else
		   {
			   PlayerAction FinalAction = null;
			   if(CheapAction == null)
			   {
				   if(TriedMoves.size()<1) return this;
			      FinalAction = TriedMoves.remove(r.nextInt(TriedMoves.size()));
			   }
			   else 
			   {
				   FinalAction = CheapAction;
			   }
			   GameState gs = GSCopy.cloneIssue(FinalAction);	
			   MCNode n = new MCNode(MaxPlayer,MinPlayer, FinalAction, gs.clone(), this );
			   ChildNodes.add(n);
			   Breadth++;
			   HasMoreAction = false;
			   return n;
			   
		   }
	   }
	   


   }
   public MCNode AddChild(int MaxPlayer,int MinPlayer) throws Exception
   {



	   PlayerAction move = GetRandomAction();
	   if(move != null)
	   {
		   GameState gs = GSCopy.cloneIssue(move);
		   MCNode n = new MCNode(MaxPlayer,MinPlayer, move, gs.clone(), this );
		   ChildNodes.add(n);
		   Breadth++;
		   return n;
	   }
	   HasMoreAction = false;
	   return this;

   }
   
   public void EvaluateMove(GameState gs, PlayerAction move)
   {
	   double h = 0;
	   double a = 0;
	   double r = 0;
	   double p = 0;
	   int size = 0;
	   for(Unit u : gs.getUnits())
	   {
		   try 
		   {
			   if(u.getPlayer() == Player && move.getAction(u) != null);
			   {
				   size++;
				   if(move.getAction(u).getType() == UnitAction.TYPE_ATTACK_LOCATION)
				   {
					   a += 1*AttackValue;
				   }
				   else if(move.getAction(u).getType() == UnitAction.TYPE_HARVEST)
				   {
					   h += 1*HarvestValue;
				   }
				   else if(move.getAction(u).getType() == UnitAction.TYPE_PRODUCE)
				   {
					   p += 1*ProduceValue;
				   }
				   else if(move.getAction(u).getType() == UnitAction.TYPE_RETURN)
				   {
					   r += 1*ReturnValue;
				   }
			   }
		   }
		   catch(NullPointerException e)
		   {
			  // System.out.println("null");
		   }
	   }
	   double result = a+h+r+p/size;
	   if(result > CheapEvaluation)
		   {
		    	CheapEvaluation = result;
		    	CheapAction = move;
		   }
	  // TriedMoves.add(move);
   }
   
   
   public void Update(double result)
   {
	   Visits += 1;
	   TotalEvaluation = result;
   }
   
   public final double GetSortValue(MCNode c)
   {
	   double d =  c.TotalEvaluation/c.Visits + Math.sqrt(2*Math.log(Visits)/c.Visits);

	   return d;
   }

   public MCNode GetMostVisitedNode()
   {
	   ChildNodes.sort((m2, m1) -> Integer.compare(m1.Visits, m2.Visits));
	   return ChildNodes.get(0);
   }
   
   public MCNode GetBestNode()
   {
	   MCNode best = ChildNodes.get(0);
	   for(MCNode mc : ChildNodes)
	   {
		   if(mc.Visits > best.Visits)
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
   
   public MCNode GetHighestEvaluationNode()
   {
	   ChildNodes.sort((m2, m1) -> Double.compare(m1.TotalEvaluation, m2.TotalEvaluation));
	   return ChildNodes.get(0);
   }
   
   public PlayerAction GetRandomAction() throws Exception
   {
	   PlayerAction move = PAG.getNextAction(CutOffTime);
	   return move;
   }
   
   public int GetVisits()
   {
	   return Visits;
   }
   
   public double GetWins()
   {
	   return TotalEvaluation;
   }
}
