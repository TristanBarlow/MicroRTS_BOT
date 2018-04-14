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
   public GameState GSCopy;
   
   public MCNode(int player, GameState gs, int MAXDEPTH)throws Exception
   {
	   {
		   Player = player;
		   GSCopy = gs;
		   PAG = new PlayerActionGenerator(GSCopy, player);
		   UntriedMoves = gs.getPlayerActions(player);
	   }
   }
   public MCNode(int player, PlayerAction move, GameState gs, MCNode parent, int depth, int MAXDEPTH)
   {
	   if(Player == 1)MinPlayer = 0;
	   Move = move;
	   Depth = depth+1;
	   ParentNode = parent;
	   UntriedMoves =  gs.getPlayerActions(player);
	   Player = player;
	   GSCopy = gs;

	  // if(Depth < MAXDEPTH) AddChild(MAXDEPTH);
   }
   
   public MCNode GetChild()
   {
	   ChildNodes.sort((m1, m2) -> Double.compare(m1.GetSortValue(this), m2.GetSortValue(this)));
	   MCNode c = ChildNodes.get(0);
	   return c;
   }
   
   public MCNode AddChild(int MAXDEPTH)
   {
	   PlayerAction move = GetRandomAction();
	   if(move != null)
	   {
	   GameState gs = GSCopy.cloneIssue(move);
	   
	   MCNode n = new MCNode(Player, move, gs.clone(), this , Depth, MAXDEPTH);
	   TriedMoves++;
	   ChildNodes.add(n);
	   return n;
	   }
	  return null;
   }
   
   public void Update(float result)
   {
	   visits += 1;
	   wins += result;
   }
   
   public final double GetSortValue(MCNode c)
   {
	   double d =  c.wins/c.visits + Math.sqrt(2*Math.log(visits)/c.visits);

	   return d;
   }
   
   public PlayerAction GetBestMove()
   {
	   
	   MCNode mostVisited = null;
       for(int i = 0;i<this.ChildNodes.size();i++) 
       {
           MCNode child = this.ChildNodes.get(i);
           if (mostVisited == null || child.visits>mostVisited.visits ||
               (child.visits==mostVisited.visits &&
                child.wins > mostVisited.wins)) 
           {
               mostVisited = child;
               System.out.println(child.visits);
           }
       }
       return mostVisited.Move;
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
}
