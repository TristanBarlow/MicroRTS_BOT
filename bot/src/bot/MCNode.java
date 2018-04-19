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
   public ArrayList<PlayerAction> DiscardedCheapMoves;
   
   public double CheapEvaluation = 0;
   public PlayerAction CheapAction;
   public int MaxCheapRuns = 10000;
   boolean CanBuildBarrakcs = false;
   
   
   private static double C = 0.05;
   private static double AttackValue = 0.2;
   private static double HarvestValue = 0.3;
   private static double ReturnValue = 0.3;
   private static double ProduceValue = 0.2;
   
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
		   DiscardedCheapMoves = new ArrayList<PlayerAction>();

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
	   DiscardedCheapMoves = new ArrayList<PlayerAction>();

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
   
   public MCNode GetChild(int MaxPlayer, int MinPlayer, boolean buildBarracks) throws Exception
   {
	   if(Depth >= MaxDepth) return this;
	   
	   CanBuildBarrakcs = buildBarracks;
	   
	   if((ChildNodes.size() < MaxBreadth) && HasMoreAction)
	   {
		   MCNode c = AddBiasChild(MaxPlayer, MinPlayer);
		   return c;
	   }
	   
	   else if(!EndGame && ChildNodes.size() > 0 )
	   {
		   ChildNodes.sort((m2, m1) -> Double.compare(m1.GetSortValue(this), m2.GetSortValue(this)));
		   MCNode c = ChildNodes.get(0);
		   return c.GetChild(MaxPlayer, MinPlayer, buildBarracks);
	   }
	   return AddRandomChild(MaxPlayer, MinPlayer);
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
	   PlayerAction FinalAction = GetBestMove();
	   GameState gs = GSCopy.cloneIssue(FinalAction);	
	   MCNode n = new MCNode(MaxPlayer,MinPlayer, FinalAction, gs.clone(), this );
	   ChildNodes.add(n);
	   return n;
   }
   
   public PlayerAction GetBestMove()
   {
	   if(DiscardedCheapMoves.size() > 0) return DiscardedCheapMoves.remove(DiscardedCheapMoves.size()-1);	   
	   
	   else if(TriedMoves.size() >0 ) return TriedMoves.remove(r.nextInt(TriedMoves.size()));
	   
	   else return new PlayerAction();
   }
   
   public MCNode AddBiasChild(int MaxPlayer,int MinPlayer) throws Exception
   {
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
			   //HasMoreAction = false;
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
			      FinalAction =  GetBestMove();
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
			  if(UnitPair.m_a.getType().canHarvest && !CanBuildBarrakcs )
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
	   if(result > CheapEvaluation || CheapEvaluation == 0)
		   {
		   		if(CheapAction != null)
		   		{
		   			DiscardedCheapMoves.add(CheapAction);
		   		}
		   		ShouldAdd = false;
		    	CheapEvaluation = result;
		    	CheapAction = move;
		   }
	   if(ShouldAdd)
	   {
		   TriedMoves.add(move);
	   }
	   else {ShouldAdd = false;}
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
