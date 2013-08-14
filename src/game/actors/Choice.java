/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.actors ;
import src.game.common.* ;
import src.user.BaseUI;
import src.util.* ;



public class Choice {
  
  
  /**  Data fields, constructors and setup-
    */
  final Actor actor ;
  final Batch <Behaviour> plans = new Batch <Behaviour> () ;
  
  
  public Choice(Actor actor) {
    this.actor = actor ;
  }
  
  
  public Choice(Actor actor, Series <Behaviour> plans) {
    this.actor = actor ;
    for (Behaviour p : plans) add(p) ;
  }
  
  
  public boolean add(Behaviour plan) {
    if (plan == null || plan.complete()) return false ;
    if (plan.nextStepFor(actor) == null) return false ;
    plans.add(plan) ;
    return true ;
  }
  
  
  
  /**  Picks a plan from those assigned earlier using priorities to weight the
    *  likelihood of their selection.
    */
  public Behaviour weightedPick(float priorityRange) {
    //
    //  Firstly, acquire the priorities for each plan.  If the permitted range
    //  of priorities is zero, simply return the most promising.
    if (BaseUI.isPicked(actor)) I.say(
      actor+" is making a choice, range: "+priorityRange
    ) ;
    float highestW = 0 ;
    int i = 0 ;
    Behaviour bestP = null ;
    final float weights[] = new float[plans.size()] ;
    for (Behaviour plan : plans) {
      final float priority = plan.priorityFor(actor) ;
      if (priority > highestW) { highestW = priority ; bestP = plan ; }
      weights[i++] = priority ;
      if (BaseUI.isPicked(actor)) I.say(
        "  "+plan+" has priority: "+priority
      ) ;
    }
    if (priorityRange == 0) return bestP ;
    //
    //  Eliminate all weights outside the permitted range, so that only plans
    //  of comparable attractiveness to the most important are considered-
    final float minPriority = Math.max(0, highestW - priorityRange) ;
    float sumWeights = 0 ; for (i = weights.length ; i-- > 0 ;) {
      weights[i] = Math.max(0, weights[i] - minPriority) ;
      sumWeights += weights[i] ;
    }
    if (sumWeights == 0) return bestP ;
    //
    //  Finally, select a candidate at random using weights based on priority-
    Behaviour picked = null ;
    float randPick = Rand.num() * sumWeights ;
    i = 0 ;
    for (Behaviour plan : plans) {
      final float chance = weights[i++] ;
      if (randPick < chance) { picked = plan ; break ; }
      else randPick -= chance ;
    }
    return picked ;
  }
}