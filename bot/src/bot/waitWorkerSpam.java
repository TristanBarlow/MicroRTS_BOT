/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package bot;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Harvest;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.ParameterSpecification;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.units.*;

/**
 *
 * @author santi
 */
public class waitWorkerSpam extends AbstractionLayerAI {    
	
  Random r = new Random();
   protected UnitTypeTable utt;
   UnitType workerType;
   UnitType baseType;
   UnitType barracksType;
   UnitType heavyType;
   
   public waitWorkerSpam(UnitTypeTable a_utt) {
       this(a_utt, new AStarPathFinding());
   }
   
   
   public waitWorkerSpam(UnitTypeTable a_utt, PathFinding a_pf) {
       super(a_pf);
       reset(a_utt);
   }

   public void reset() {
   	super.reset();
   }
   
   public void reset(UnitTypeTable a_utt)  
   {
       utt = a_utt;
       workerType = utt.getUnitType("Worker");
       baseType = utt.getUnitType("Base");
       barracksType = utt.getUnitType("Barracks");
       heavyType = utt.getUnitType("Heavy");
   }      

   public AI clone() {
       return new waitWorkerSpam(utt, pf);
   }
    
    @Override
    public PlayerAction getAction(int player, GameState gs) 
    {
    	
    	  PhysicalGameState pgs = gs.getPhysicalGameState();
          Player p = gs.getPlayer(player);
          PlayerAction pa = new PlayerAction();
          
          // behavior of bases:
          for(Unit u:pgs.getUnits()) {
              if (u.getType()==baseType && 
                  u.getPlayer() == player && 
                  gs.getActionAssignment(u)==null) {
                  baseBehavior(u,p,pgs);
              }
          }

          // behavior of workers:
          List<Unit> workers = new LinkedList<Unit>();
          for(Unit u:pgs.getUnits()) {
              if (u.getType().canHarvest && 
                  u.getPlayer() == player) {
                  workers.add(u);
              }        
          }
          workersBehavior(workers,p,gs);
          
                  
          return translateActions(player,gs);
      
    }

    public void attackNearestEnemy(Player p, GameState gs, Unit u)
    {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int closestDistance = 0;
        for(Unit u2:pgs.getUnits()) {
            if (u2.getPlayer()>=0 && u2.getPlayer()!=p.getID()) { 
                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                if (closestEnemy==null || d<closestDistance) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
        }
        if (closestEnemy!=null) {
            attack(u,closestEnemy);
        }
    }
    
    public void workersBehavior(List<Unit> workers, Player p, GameState gs) 
    {
    	int workerThreshHold = 7;
    	PhysicalGameState pgs = gs.getPhysicalGameState();
    	List<Unit> careTakers = new LinkedList<Unit>();
    	List<Unit> AgroWorkers = new LinkedList<Unit>();
    	
    	if(workers.size()>= workerThreshHold)
    	{
    		AgroWorkers = workers;
    		for(Unit u:AgroWorkers) attackNearestEnemy(p, gs , u);
    	}
    	else
    	{
    		for (Unit u : workers) {
                Unit closestBase = null;
                Unit closestResource = null;
                int closestDistance = 0;
                for (Unit u2 : pgs.getUnits()) {
                    if (u2.getType().isResource) {
                        int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                        if (closestResource == null || d < closestDistance) {
                            closestResource = u2;
                            closestDistance = d;
                        }
                    }
                }
                closestDistance = 0;
                for (Unit u2 : pgs.getUnits()) {
                    if (u2.getType().isStockpile && u2.getPlayer()==p.getID()) {
                        int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                        if (closestBase == null || d < closestDistance) {
                            closestBase = u2;
                            closestDistance = d;
                        }
                    }
                }
                if (closestResource != null && closestBase != null) {
                    AbstractAction aa = getAbstractAction(u);
                    if (aa instanceof Harvest) {
                        Harvest h_aa = (Harvest)aa;
                        if (h_aa.getTarget() != closestResource || h_aa.getBase()!=closestBase) harvest(u, closestResource, closestBase);
                    } else {
                        harvest(u, closestResource, closestBase);
                    }
                }
    		}
    	}
	}


	public void baseBehavior(Unit u, Player p, PhysicalGameState pgs) 
	{
	       if (p.getResources()>=workerType.cost) train(u, workerType);
	}


	@Override
    public List<ParameterSpecification> getParameters()
    {
        return new ArrayList<>();
    }
    
}
