package bot;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import ai.evaluation.EvaluationFunction;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.*;


public class MCEvaluation extends EvaluationFunction {    
    public static float ResourceValue = 20;
    public static float BarracksValue = 10;
    public static float ResourceInWorker = 10;
    public static float BaseValue = 20;
    public static float RangedValue = 80;
    public static float WorkerValue = 1000000000;
    public static float LightValue =60;
    public static float UnitBonus = 40;
    public static float HeavyValue =70;
    
    UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType heavyType;
    UnitType lightType;
    UnitType resourceType;
    UnitType rangedType;
    
    public MCEvaluation(UnitTypeTable utt)
    {
        workerType = utt.getUnitType("Worker");
        baseType = utt.getUnitType("Base");
        barracksType = utt.getUnitType("Barracks");
        heavyType = utt.getUnitType("Heavy");
        lightType = utt.getUnitType("Light");
        resourceType = utt.getUnitType("Resource");
        rangedType = utt.getUnitType("Ranged");
    }
    
    public float evaluate(int maxplayer, int minplayer, GameState gs) {
        float s1 = base_score(maxplayer,gs);
        float s2 = base_score(minplayer,gs);
        float total = s1+s2;
        return  (s1/total) - (s2/total);
    }
    
    public float base_score(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        float score = gs.getPlayer(player).getResources()*ResourceValue;
        boolean anyunit = false;
        for(Unit u:pgs.getUnits()) {
            if (u.getPlayer()==player) 
            {
                anyunit = true;
                score += u.getResources() * ResourceInWorker;
                score += UnitBonus * u.getCost()*Math.sqrt( u.getHitPoints()/u.getMaxHitPoints());
             //   if(u.getType() == baseType) { score += BaseValue * u.getCost()*Math.sqrt( u.getHitPoints()/u.getMaxHitPoints()); break;}
               // if(u.getType() == workerType) { score += WorkerValue * u.getCost()*Math.sqrt( u.getHitPoints()/u.getMaxHitPoints());break;}
                //if(u.getType() == barracksType) { score += BarracksValue* u.getCost()*Math.sqrt( u.getHitPoints()/u.getMaxHitPoints());break;}
                //if(u.getType() == rangedType) { score += RangedValue* u.getCost()*Math.sqrt( u.getHitPoints()/u.getMaxHitPoints());break;}
                //if(u.getType() == lightType) { score += LightValue* u.getCost()*Math.sqrt( u.getHitPoints()/u.getMaxHitPoints());break;}
                //if(u.getType() == heavyType) { score += HeavyValue* u.getCost()*Math.sqrt( u.getHitPoints()/u.getMaxHitPoints());break;}
                
            }
        }
        if (!anyunit) return 0;
        return score;
    }    
    
    public float upperBound(GameState gs) {
        return 1.0f;
    }
}

