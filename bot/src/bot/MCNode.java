package bot;

import java.util.ArrayList;
import java.util.Random;
import rts.GameState;
import rts.PlayerAction;
import rts.PlayerActionGenerator;


public class MCNode
{
   public PlayerAction Move;
   public MCNode ParentNode;
   public GameState GSCopy;
   public ArrayList<MCNode>ChildNodes = new ArrayList<MCNode>();
   
   private static double C = 0.05;
   private Random r = new Random();
 
   public double TotalEvaluation =0;
   public double Evaluation = 0;
   public int Visits = 0;
   
   public int MaxDepth = 10;
   public int Depth = 1;
   
   public int MaxBreadth =0;
   public int Breadth = 0;
   
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

   }
   
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
	   if(!HasMoreAction)return this;
	   if(Depth >= MaxDepth) return this;
	   
	   if((ChildNodes.size() < MaxBreadth))
	   {
		   MCNode c = AddChild(MaxPlayer, MinPlayer); 
		   return c;
	   }
	   
	   else if(!EndGame && ChildNodes.size() > 0)
	   {
		   ChildNodes.sort((m2, m1) -> Double.compare(m1.GetSortValue(this), m2.GetSortValue(this)));
		   MCNode c = ChildNodes.get(0);
		   return c.GetChild(MaxPlayer, MinPlayer);
	   }
	   return this;
   }
   
   public MCNode EpislonChildGet(int MaxPlayer, int MinPlayer, float E0, float eg ) throws Exception
   {
	   if(ChildNodes.size() > 0 && r.nextFloat() <= E0 )
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
   
   public MCNode AddChild(int MaxPlayer,int MinPlayer) throws Exception
   {
	   PlayerAction move;
	   move = GetRandomAction();
	   
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
