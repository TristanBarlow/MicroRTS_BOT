package bot;

import ai.core.AI;
import ai.RandomBiasedAI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import ai.evaluation.EvaluationFunction;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import ai.mcts.uct.UCTNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import rts.GameState;
import rts.PlayerAction;
import rts.PlayerActionGenerator;
import rts.units.UnitTypeTable;
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
   public ArrayList<MCNode>ChildNodes = new ArrayList<MCNode>();
   public float wins =0;
   public int visits = 0;
   public int Depth = 0;
   public List<PlayerAction> UntriedMoves;
   public PlayerActionGenerator PAG = null;
   public int Player = 0;
   public int MinPlayer =1;
   public int TriedMoves = 0;
   public boolean EndGame = false;
   public float AverageEvaluation = 0;
   public GameState GSCopy;
   
   public MCNode(int player, GameState gs, int MAXDEPTH)throws Exception
   {
		   Player = player;
		   GSCopy = gs;
		   while(!gs.canExecuteAnyAction(player) && !GSCopy .gameover() )
		   {
			   gs.cycle();
		   }
		   if(!gs.gameover()&& gs.winner() == -1 )
		   {
			   
			   UntriedMoves =  gs.getPlayerActions(player);
		   }
		   if(gs.winner() != player)
		   {
			   UntriedMoves =  gs.getPlayerActions(player);
			   EndGame = true;
		   }
		  // while(UntriedMoves.size()>0 && ChildNodes.size() < MAXBREADTH) AddChild(MAXDEPTH);
   }
   
   public MCNode(int player, PlayerAction move, GameState gs, MCNode parent, int depth, int MAXDEPTH) throws Exception
   {
	   if(Player == 1)MinPlayer = 0;
	   Move = move;
	   Depth = depth+1;
	   ParentNode = parent;
	   Player = player;
	   GSCopy = gs;
	   while(!GSCopy.canExecuteAnyAction(player) && !GSCopy.gameover() )
	   {
		   GSCopy.cycle();
	   }
	   if(!GSCopy.gameover()&& GSCopy.winner() == -1 )
	   {
		   UntriedMoves =  GSCopy.getPlayerActions(player);
	   }
   }
   
   public float GetAverageEvaluation()
   {
	   return wins/visits;
   }
   
   public MCNode GetChild()
   {
	   ChildNodes.sort((m2, m1) -> Double.compare(m1.GetSortValue(this), m2.GetSortValue(this)));
	   MCNode c = ChildNodes.get(0);
	   return c;
   }
   
   public MCNode AddChild(int MAXDEPTH) throws Exception
   {
	   PlayerAction move = GetRandomAction();
	   if(move != null && Depth <= MAXDEPTH)
	   {
	   GameState gs = GSCopy.cloneIssue(move);
	   MCNode n = new MCNode(Player, move.clone(), gs.clone(), this , Depth, MAXDEPTH);
	   TriedMoves++;
	   ChildNodes.add(n);
	   return n;
	   }
	   else {return this;}
   }
   
   public void Update(float result)
   {
	   visits += 1;
	   wins += result;
   }
   
   public final double GetSortValue(MCNode c)
   {
	   double d =  wins/visits + Math.sqrt(2*Math.log(c.visits)/visits);

	   return d;
   }

   public MCNode GetMostVisitedNode()
   {
	   ChildNodes.sort((m2, m1) -> Double.compare(m1.visits, m2.visits));
	   return ChildNodes.get(0);
   }
   
   public MCNode GetChild2()
   {
	   // Bandit policy:
       double best_score = 0;
       MCNode best = null;
       for (MCNode child : ChildNodes) 
       {
           double tmp = childValue(child);
           if (best==null || tmp>best_score) {
               best = child;
               best_score = tmp;
           }
       }
       if(best == null)return this;
       return best;
   }
   
   public PlayerAction GetRandomAction()
   {
	   Random r = new Random();
	   return UntriedMoves.remove((r.nextInt(UntriedMoves.size())));
   }
   public int GetVisits()
   {
	   return visits;
   }
   
   public float GetWins()
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

       double tmp = 0.005*exploitation + exploration;
       return tmp;
   }
}
