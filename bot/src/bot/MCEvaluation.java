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
    public static float ResourceValue = 0;
    public static float BarracksValue = 0;
    public static float ResourceInWorker = 0;
    public static float BaseValue = 0;
    public static float RangedValue = 0;
    public static float WorkerValue = 0;
    public static float HeavyValue =0;
    public static float LightValue =0;
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
        float totalScore =  s1+s2;
        float S1Ratio = s1/totalScore;
        float S2Ratio = s2/totalScore;
        float ReturnScore = S1Ratio - S2Ratio;
        return ReturnScore;
    }
    
    public float base_score(int player, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        float score = gs.getPlayer(player).getResources()*ResourceValue;
        for(Unit u:pgs.getUnits()) 
        {
            if (u.getPlayer()==player) 
            {
            	score += u.getResources()*ResourceInWorker;
            	if(u.getType() == workerType) { score+= WorkerValue; break;}
            	if(u.getType() == barracksType) {score+= BarracksValue;break;}
            	if(u.getType() == lightType) {score+= LightValue;break;}
            	if(u.getType() == RangedType) { score+= RangedValue;break;}
            	if(u.getType() == baseType) { score+= BaseValue;break;}
            	if(u.getType() == heavyType) { score+=HeavyValue ;break;}
            }
        }
        return score;
    }    
    
    public float upperBound(GameState gs) {
        return 1.0f;
    }
}

