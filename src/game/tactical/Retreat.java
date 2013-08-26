



package src.game.tactical ;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.user.* ;
import src.util.* ;



public class Retreat extends Plan implements ActorConstants {
  
  
  
  /**  Constants, field definitions, constructors and save/load methods-
    */
  Target safePoint = null ;
  
  
  public Retreat(Actor actor) {
    super(actor) ;
  }


  public Retreat(Session s) throws Exception {
    super(s) ;
    this.safePoint = s.loadTarget() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveTarget(safePoint) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public float priorityFor(Actor actor) {
    float danger = dangerAtSpot(actor.origin(), actor, actor.AI.seen()) ;
    return PARAMOUNT * danger ;
  }
  
  
  protected Behaviour getNextStep() {
    if (actor.aboard() == safePoint) return null ;
    if (safePoint == null) {
      safePoint = nearestHaven(actor, null) ;
    }
    final Action flees = new Action(
      actor, safePoint,
      this, "actionFlee",
      Action.LOOK, "Fleeing to "+safePoint
    ) ;
    flees.setProperties(Action.QUICK) ;
    return flees ;
  }
  
  
  public boolean actionFlee(Actor actor, Target safePoint) {
    return true ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("Retreating to ") ;
    d.append(safePoint) ;
  }
  
  
  
  /**  These methods select safe points to withdraw to in a local area.
    */
  public static Tile pickWithdrawPoint(
    Actor actor, float range,
    Target target, float salt
  ) {
    final int numPicks = 3 ;  //Make this an argument, instead of range?
    Tile pick = actor.origin() ;
    float bestRating = dangerAtSpot(pick, actor, actor.AI.seen()) ;
    for (int i = numPicks ; i-- > 0 ;) {
      final Tile tried = Spacing.pickRandomTile(actor, range, actor.world()) ;
      if (tried == null) continue ;
      if (Spacing.distance(tried, target) > range * 2) continue ;
      float tryRating = dangerAtSpot(tried, actor, actor.AI.seen()) ;
      tryRating += (Rand.num() - 0.5f) * salt ;
      if (tryRating < bestRating) { bestRating = tryRating ; pick = tried ; }
    }
    return pick ;
  }
  
  
  public static float dangerAtSpot(
    Target spot, Actor actor, Batch <Element> seen
  ) {
    //
    //  Get a reading of threats based on all actors visible to this one, and
    //  their distance from the spot in question.
    ///I.say(actor+" getting danger at: "+spot) ;
    float seenDanger = 0 ;
    final float range = World.DEFAULT_SECTOR_SIZE ;
    for (Element m : seen) {
      if (m == actor || ! (m instanceof Actor)) continue ;
      final Actor near = (Actor) m ;
      //
      //  More distant foes are less threatening.
      float danger = Combat.combatStrength(near) ;
      if (! near.isDoing(Combat.class)) danger /= 2 ;
      final float dist = Visit.clamp(Spacing.distance(spot, near), 0, range) ;
      danger *= 1 - (dist / range) ;
      
      float attitude = actor.AI.relation(near) ;
      ///I.say("Danger is: "+danger+", attitude: "+attitude+" for: "+near) ;
      if (attitude < 0) seenDanger += danger ;
      if (attitude > 0) seenDanger -= danger / 2 ;
    }
    if (seenDanger == 0) return 0 ;
    final float strength = Combat.combatStrength(actor) * 2 ;
    if (strength == 0) return PARAMOUNT ;
    return Visit.clamp(seenDanger / strength, 0, PARAMOUNT) ;
  }
  
  
  
  /**  These methods select safe venues to run to, over longer distances.
    */
  public static Target nearestHaven(Actor actor, Class prefClass) {
    //
    //  TODO:  Use the list of venues the actor is aware of.
    final Presences p = actor.world().presences ;
    int numC = 3 ;
    
    Object picked = null ;
    float bestRating = 0 ;
    int numChecked = 0 ;
    
    if (actor.AI.home() != null) {
      final Venue home = actor.AI.home() ;
      float rating = rateHaven(home, actor, prefClass) ;
      if (rating > bestRating) { bestRating = rating ; picked = home ; }
    }
    if (actor.base() != null) {
      for (Object t : p.matchesNear(actor.base(), actor, -1)) {
        if (numChecked++ > numC) break ;
        float rating = rateHaven(t, actor, prefClass) ;
        if (rating > bestRating) { bestRating = rating ; picked = t ; }
      }
    }
    if (prefClass != null) {
      numChecked = 0 ;
      for (Object t : p.matchesNear(prefClass, actor, -1)) {
        if (numChecked++ > numC) break ;
        float rating = rateHaven(t, actor, prefClass) ;
        if (rating > bestRating) { bestRating = rating ; picked = t ; }
      }
    }
    if (picked == null) picked = pickWithdrawPoint(
      actor, actor.health.sightRange(), actor, 0.1f
    ) ;
    
    return (Target) picked ;
  }
  
  
  private static float rateHaven(Object t, Actor actor, Class prefClass) {
    //
    //  TODO:  Don't pick anything too close by either.  That'll be in a
    //  dangerous area.
    if (! (t instanceof Venue)) return 1 ;
    final Venue haven = (Venue) t ;
    float rating = 1 ;
    if (haven.getClass() == prefClass) rating *= 2 ;
    if (haven.base() == actor.base()) rating *= 2 ;
    if (haven == actor.AI.home()) rating *= 2 ;
    final int SS = Terrain.SECTOR_SIZE ;
    rating *= SS / (SS + Spacing.distance(actor, haven)) ;
    return rating ;
  }
}


