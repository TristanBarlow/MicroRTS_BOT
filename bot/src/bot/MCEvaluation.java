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
    public static float ResourceValue = 30;
    public static float BarracksValue = 200;
    public static float ResourceInWorker = 10;
    public static float BaseValue = 150;
    public static float RangedValue = 50;
    public static float WorkerValue = 10;
    public static float HeavyValue =100;
    public static float LightValue =100;
    public static float UnitBonus = 40;
    UnitType workerType;
    UnitType baseType;
    UnitType barracksType;
    UnitType heavyType;
    UnitType lightType;
    UnitType ResourceType;
    UnitType RangedType;
    
    public MCEvaluation(UnitTypeTable utt)
    {
        workerType = utt.getUnitType("Worker");
        baseType = utt.getUnitType("Base");
        barracksType = utt.getUnitType("Barracks");
        heavyType = utt.getUnitType("Heavy");
        lightType = utt.getUnitType("Light");
        ResourceType = utt.getUnitType("Resource");
        RangedType = utt.getUnitType("Ranged");
    }
    
    public float evaluate(int maxplayer, int minplayer, GameState gs) {
        float s1 = base_score(maxplayer,gs);
        float s2 = base_score(minplayer,gs);
        if (s1 + s2 == 0) return 0.5f;
        return  (2*s1 / (s1 + s2))-1;
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
                if(u.getType() == baseType) score += BaseValue * u.getCost()*Math.sqrt( u.getHitPoints()/u.getMaxHitPoints());
                //if(u.getType() == barracksType) score += BarracksValue * u.getCost()*Math.sqrt( u.getHitPoints()/u.getMaxHitPoints());
                
            }
        }
        if (!anyunit) return 0;
        return score;
    }    
    
    public float upperBound(GameState gs) {
        return 1.0f;
    }
}

