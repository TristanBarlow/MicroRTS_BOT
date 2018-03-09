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
   List<Integer> reservedPositions;
   UnitType workerType;
   UnitType baseType;
   UnitType barracksType;
   UnitType heavyType;
   UnitType lightType;
   UnitType ResourceType;
   UnitType RangedType;
   
   int resourcesLeft;
   
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
       lightType = utt.getUnitType("Light");
       ResourceType = utt.getUnitType("Resource");
       RangedType = utt.getUnitType("Ranged");
       
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
         reservedPositions = new LinkedList<Integer>();
          
          boolean nearestGot = false;
          Unit enemyUnit = null;
          
          List<Unit> workers = new LinkedList<Unit>();
          List<Unit> heavys = new LinkedList<Unit>();
          List<Unit> lights = new LinkedList<Unit>();
          List<Unit> bases = new LinkedList<Unit>();
          List<Unit> barracksTypes = new LinkedList<Unit>();
          List<Unit> resources = new LinkedList<Unit>();
          List<Unit> rangers = new LinkedList<Unit>();
          
    	  resourcesLeft = p.getResources();
    	  boolean sendToAttack;
          
          // behavior of bases:
          for(Unit u:pgs.getUnits()) 
         {
              if (u.getPlayer() == player && gs.getActionAssignment(u)==null) 
              {
         		  if(!nearestGot)
        		  {
        			  enemyUnit = getNearestEnemy(gs.getPhysicalGameState(), p, u);
        			  nearestGot = true;
        		  }
            	  switch(u.getType().name)
            	  {
            	  case "Worker":
            		  workers.add(u);
            		  break;
            		  
            	  case "Base":
            		  bases.add(u);
            		  baseBehavior(u, p, pgs);
            		  break;
            		  
            	  case "Barracks":
            		  barracksTypes.add(u);
            		  break;
            		  
            	  case "Heavy":
            		  heavys.add(u);
            		  attack(u, enemyUnit);
            		  break;
            		  
            	  case "Ranged":
            		  rangers.add(u);
            		  attack(u, enemyUnit);
            		  break;
            		  
            	  case "Resource":
            		  resources.add(u);
            		  break;
            		  
            	  case "light":
            		  lights.add(u);
            		  attack(u, enemyUnit);
            		  break;
            		  
            	  }
              }
          }
          
   	   if(workers.size() >0)
   	   {   
           if(bases.size() < 1) 
           {

        	   buildBase(workers.get(0), p, pgs);
        	   workers.remove(0);
        	   }
           
           if (workers.size() > 1)
           { 
        	   for(Unit u: workers)
        	   {
        		   attack(u,enemyUnit);
        	   }
        	   
           }
           else sendWorkersToMine((workers), pgs, p);
   	   }
   	   
        for(Unit u: bases)
        {
        	baseBehavior(u, p, pgs);
        }
          return translateActions(player,gs);
      
    }

    public void buildBase(Unit u, Player p, PhysicalGameState pgs) 
    {
    	 if (resourcesLeft >= baseType.cost) 
    	 {
             buildIfNotAlreadyBuilding(u,baseType,u.getX(),u.getY(),reservedPositions,p,pgs);
             resourcesLeft -= baseType.cost;
         }
    }
    
    public Unit getNearestEnemy(PhysicalGameState pgs, Player p, Unit u)
    {
    	 Unit closestEnemy = null;
         int closestDistance = 0;
         for(Unit u2:pgs.getUnits()) 
         {
             if (u2.getPlayer()>=0 && u2.getPlayer()!=p.getID()) { 
                 int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                 if (closestEnemy==null || d<closestDistance) {
                     closestEnemy = u2;
                     closestDistance = d;
                 }
             }
         }
         if (closestEnemy!=null)
         {
             return closestEnemy;
         }
         else
         {
        	 return null;
         }
    }
    
    public void attackNearestEnemy(Player p, GameState gs, Unit u)
    {
        PhysicalGameState pgs = gs.getPhysicalGameState();
            attack(u,getNearestEnemy(pgs, p, u));
    }
    
    public void allAttackNearest(List<Unit> workers, Player p, GameState gs)
    {
    	 PhysicalGameState pgs = gs.getPhysicalGameState();
    	Unit enemyToAttack = getNearestEnemy(pgs, p, workers.get(0));
    	for (Unit u : workers)
    	{
    		attack(u, enemyToAttack);
    	}
    }
    
    public void sendWorkersToMine(List<Unit> workers, PhysicalGameState pgs, Player p)
    {

		for (Unit u : workers)
		{
            Unit closestBase = null;
            Unit closestResource = null;
            int closestDistance = 0;
            for (Unit u2 : pgs.getUnits()) 
            {
                if (u2.getType().isResource) 
                {
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestResource == null || d < closestDistance) 
                    {
                        closestResource = u2;
                        closestDistance = d;
                    }
                }
            }
            closestDistance = 0;
            for (Unit u2 : pgs.getUnits()) 
            {
                if (u2.getType().isStockpile && u2.getPlayer()==p.getID())
                {
                    int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
                    if (closestBase == null || d < closestDistance) 
                    {
                        closestBase = u2;
                        closestDistance = d;
                    }
                }
            }
            if (closestResource != null && closestBase != null) 
            {
                AbstractAction aa = getAbstractAction(u);
                if (aa instanceof Harvest) 
                {
                    Harvest h_aa = (Harvest)aa;
                    if (h_aa.getTarget() != closestResource || h_aa.getBase()!=closestBase) harvest(u, closestResource, closestBase);
                } else 
                {
                    harvest(u, closestResource, closestBase);
                }
            }
		}
	
    }
    
    public void workersBehavior(List<Unit> workers, Player p, GameState gs) 
    {
    	int workerThreshHold = 0;
    	PhysicalGameState pgs = gs.getPhysicalGameState();
    	List<Unit> careTakers = new LinkedList<Unit>();
    	List<Unit> AgroWorkers = new LinkedList<Unit>();
    	
    	if(workers.size()> workerThreshHold)
    	{
    		AgroWorkers = workers;
    		allAttackNearest(workers,p ,gs );
    		//for(Unit u:AgroWorkers) attackNearestEnemy(p, gs , u);
    	}
    	else
    	{
    		
    	}
	}


	public void baseBehavior(Unit u, Player p, PhysicalGameState pgs) 
	{
		if(resourcesLeft >= workerType.cost )
			{
			train(u,  workerType);
			resourcesLeft -= workerType.cost;
			}
	}


	@Override
    public List<ParameterSpecification> getParameters()
    {
        return new ArrayList<>();
    }
    
}
