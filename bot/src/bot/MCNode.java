package bot;

import ai.core.AI;
import ai.RandomBiasedAI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.mcts.naivemcts.UnitActionTableEntry;
import ai.mcts.uct.UCTNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import rts.GameState;
import rts.PlayerAction;
import rts.PlayerActionGenerator;
import rts.ResourceUsage;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitTypeTable;
import util.Pair;
import ai.core.InterruptibleAI;
import ai.portfolio.*;
import ai.minimax.*;
import ai.abstraction.*;
import ai.abstraction.pathfinding.GreedyPathFinding;
import ai.RandomAI;
import java.util.Map;
import java.util.Comparator;
import java.math.*;

public class MCNode
{
   public PlayerAction Move;
   public MCNode ParentNode;
   public GameState GSCopy;
   public ArrayList<MCNode>ChildNodes = new ArrayList<MCNode>();
   private ArrayList<MCUnitActions> UnitActions;
   
   private static double C = 0.5;
   private Random r = new Random();
   
   public double wins =0;
   public int visits = 0;
   
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
   public float AverageEvaluation = 0;
   
   
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
	   return wins/visits;
   }
   
   public MCNode GetChild(int MaxPlayer, int MinPlayer) throws Exception
   {
	   if(ChildNodes.size() < MaxBreadth && HasMoreAction)
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
   
   public MCNode AddChild(int MaxPlayer,int MinPlayer) throws Exception
   {
	   PlayerAction move;
	   move = GetRandomAction();
	   
	   if(move != null && Depth <= MaxDepth)
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
	   visits += 1;
	   wins = result;
   }
   
   public final double GetSortValue(MCNode c)
   {
	   double d =  wins/visits + Math.sqrt(2*Math.log(c.visits)/visits);

	   return d;
   }

   public MCNode GetMostVisitedNode()
   {
	   ChildNodes.sort((m2, m1) -> Integer.compare(m1.visits, m2.visits));
	   return ChildNodes.get(0);
   }
   
   public MCNode GetBestNode()
   {
	   MCNode best = ChildNodes.get(0);
	   for(MCNode mc : ChildNodes)
	   {
		   if(mc.visits > best.visits)
		   {
			   best = mc;
		   }
		   else if(mc.visits == best.visits && mc.wins > best.wins)
		   {
			   best = mc;
		   }
	   }
	   return best;
   }
   
   public MCNode GetHighestEvaluationNode()
   {
	   ChildNodes.sort((m2, m1) -> Double.compare(m1.wins, m2.wins));
	   return ChildNodes.get(0);
   }
   
   public PlayerAction GetRandomAction() throws Exception
   {
	   PlayerAction move = PAG.getNextAction(CutOffTime);
	   return move;
   }
   
   public int GetVisits()
   {
	   return visits;
   }
   
   public double GetWins()
   {
	   return wins;
   }
   public double childValue(MCNode child) 
   {
       double exploitation = ((double)child.wins) / child.visits;
       double exploration = Math.sqrt(Math.log((double)visits)/child.visits);
       if (EndGame) 
       {
           // max node:
           exploitation = (1 + exploitation)/(2*1);
       } 
       else 
       {
           exploitation = (1 - exploitation)/(2*1);
       }
//           System.out.println(exploitation + " + " + exploration);

       double tmp = C*exploitation + exploration;
       return tmp;
   }
}
