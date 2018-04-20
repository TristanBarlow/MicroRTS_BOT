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
   
   public ArrayList<MCActionValue> HighPotenialMoves;
   public ArrayList<PlayerAction> RandomMoveSample;
   
   public double CheapEvaluation = 0.2;
   boolean CanBuildBarrakcs = false;
   private int MaxSampledMoves = 100;
   private int MaxRandomMoves = 50;
   private int MaxSampleIterations = 5000;
   private boolean ShouldSampleMoves = true;
   
   
   
   private static double C = 0.05;
   private static double AttackValue = 2;
   private static double HarvestValue = 5;
   private static double ReturnValue = 1;
   private static double ProduceValue = 1;
   
   private Random r = new Random();
 
   public double TotalEvaluation =0;
   public double Evaluation = 0;
   public int Visits = 0;
   
   public int MaxDepth = 10;
   public int Depth = 1;
   
   public int NumberOfUnits = 0;
   
   public int MaxBreadth =0;
   public int Breadth = 0;
   
   public PlayerActionGenerator PAG = null;
   public boolean HasMoreAction = false;
   
   public int Player = 0;
   public int WinningPlayer = -1;
   public long CutOffTime = 0;

   public boolean EndGame = false;
   
   
   //RootNode only
   public MCNode(int MaxPlayer, int MinPlayer, GameState gs, int MAXDEPTH, int breadth, long cutOffTime, int MaxBiasSamples)throws Exception
   {
		   Player = MaxPlayer;
		   GSCopy = gs;
		   MaxDepth = MAXDEPTH;
		   MaxBreadth = breadth;
		   CutOffTime = cutOffTime;
		   MaxSampledMoves = MaxBiasSamples;
		   while(!GSCopy.canExecuteAnyAction(MaxPlayer) && !GSCopy.gameover())
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
	   PAG.randomizeOrder();
	   ChildNodes = new ArrayList<MCNode>();
	   HighPotenialMoves = new ArrayList<MCActionValue>();
	   CheapEvaluation = GetCheapEvaluationBound(CheapEvaluation);
	   RandomMoveSample = new ArrayList<PlayerAction>();
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
   
   public MCNode GetChild(int MaxPlayer, int MinPlayer, boolean buildBarracks) throws Exception
   {
	   if(Depth >= MaxDepth) return this;
	   
	   CanBuildBarrakcs = buildBarracks;
	   

	   if(!EndGame && ChildNodes.size() > 0  && r.nextDouble() >= 0.4)
	   {
		   //ChildNodes.sort((m2, m1) -> Double.compare(m1.GetSortValue(this), m2.GetSortValue(this)));
		   //MCNode c = ChildNodes.get(0);
		   //return c.GetChild(MaxPlayer, MinPlayer, buildBarracks);
		   return GetGreedyChild(0, MaxPlayer, MinPlayer).GetChild(MaxPlayer, MinPlayer, buildBarracks);
	   }
	   else if(HasMoreAction)
	   {
		   return AddQuickBiasChild(MaxPlayer, MinPlayer);
	   }
	   
	   return this;
   }
   
   public MCNode GetGreedyChild(double EG, int MaxPlayer, int MinPlayer)   
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
	   PlayerAction FinalAction = GetMove();
	   GameState gs = GSCopy.cloneIssue(FinalAction);	
	   MCNode n = new MCNode(MaxPlayer,MinPlayer, FinalAction, gs.clone(), this );
	   ChildNodes.add(n);
	   return n;
   }
   
   public PlayerAction GetMove() throws Exception
   {
	   if(HighPotenialMoves.size() > 0)
	   {
	   		return HighPotenialMoves.remove(0).GetPlayerAction();
	   }
	   
	   if(RandomMoveSample.size() > 0) return RandomMoveSample.remove(r.nextInt(RandomMoveSample.size()));
	   
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
		   ChildNodes.add(n);
		   Breadth++;
		   return n;
	   }
	   HasMoreAction = false;
	   return this;

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
	   if(result > CheapEvaluation && HighPotenialMoves.size() < MaxSampledMoves && ShouldAdd)
	   {
	   		HighPotenialMoves.add(new MCActionValue(move, result));
	   		return result;
	   }
	   else if(RandomMoveSample.size() < MaxRandomMoves  && ShouldAdd)
	   {
		   RandomMoveSample.add(move);
		   return result;
	   }
	   return result;
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
   
   public int GetVisits()
   {
	   return Visits;
   }
   
   public double GetWins()
   {
	   return TotalEvaluation;
   }
}
