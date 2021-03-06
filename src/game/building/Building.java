


package src.game.building ;
import src.game.common.* ;
import src.game.actors.* ;
import src.user.* ;
import src.util.* ;





public class Building extends Plan implements ActorConstants {
  
  
  
  /**  Field definitions, constants and save/load methods-
    */
  private static boolean verbose = false ;
  
  final Venue built ;
  
  
  public Building(Actor actor, Venue repaired) {
    super(actor, repaired) ;
    this.built = repaired ;
  }
  
  
  public Building(Session s) throws Exception {
    super(s) ;
    built = (Venue) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(built) ;
  }
  
  
  
  /**  Assessing targets and priority-
    */
  public float priorityFor(Actor actor) {
    //
    //  By default, the impetus to repair something is based on qualification
    //  for the task and personality.
    ///I.sayAbout(built, "Considering repair of "+built+"?") ;
    final float attachment = Math.max(
      actor.base().communitySpirit(),
      actor.mind.relation(built)
    ) ;
    //
    //  TODO:  Factor out the skill evaluation down below?
    float skillRating = actor.traits.chance(ASSEMBLY, MODERATE_DC) ;
    skillRating += actor.traits.chance(HARD_LABOUR, ROUTINE_DC) ;
    skillRating /= 2 ;
    
    final boolean broke = built.base().credits() <= 0 ;
    float impetus = skillRating / 2f ;
    impetus -= actor.traits.traitLevel(NATURALIST) / 10f ;
    impetus -= actor.traits.traitLevel(INDOLENT) / 10f ;
    if (broke || impetus <= 0 || attachment <= 0) impetus = 0 ;
    else impetus *= attachment ;
    //
    //  If damage is higher than 50%, priority converges to maximum, regardless
    //  of competency, but only when you have altruistic motives.
    float needRepair = (1 - built.structure.repairLevel()) * 1.5f ;
    if (! built.structure.intact()) needRepair = 1.0f ;
    if (built.structure.needsUpgrade()) needRepair += 0.5f ;
    if (built.structure.burning()) needRepair++ ;
    if (verbose) I.sayAbout(
      built, "Considering repair of "+built+", need: "+needRepair+
      "\nimpetus/skill for "+actor+" is "+impetus+"/"+skillRating
    ) ;
    if (needRepair > 0.5f) {
      if (needRepair > 1) needRepair = 1 ;
      final float urgency = (needRepair - 0.5f) * 2 ;
      impetus += (1 - impetus) * urgency * attachment * (1 + skillRating) / 2 ;
      if (verbose) I.sayAbout(actor, "Attachment "+attachment) ;
      if (verbose) I.sayAbout(actor, "Need for repair: "+needRepair) ;
    }
    else if (needRepair == 0) return 0 ;
    //else needRepair = (needRepair + 0.5f) / 2 ;
    //
    //  During initial consideration, include competition as a decision factor,
    //  so that you don't get dozens of actors converging on a minor breakdown.
    float competition = 0 ;
    if (! begun()) {
      competition = Plan.competition(Building.class, built, actor) ;
      if (verbose) I.sayAbout(built, "Competition is: "+competition) ;
      competition /= 1 + (built.structure.maxIntegrity() / 100f) ;
    }
    //
    //  Finally, scale, offset and clamp appropriately-
    impetus = (impetus * needRepair * URGENT) - competition ;
    impetus -= Plan.rangePenalty(actor, built) ;
    if (! broke) impetus += priorityMod ;
    if (verbose) I.sayAbout(actor, "Priority for "+this+" is "+impetus) ;
    return Visit.clamp(impetus, 0, URGENT) ;
  }
  
  
  public static Building getNextRepairFor(Actor client, float priorityMod) {
    final World world = client.world() ;
    final Batch <Venue> toRepair = new Batch <Venue> () ;
    world.presences.sampleFromKeys(
      client, world, 10, toRepair, "damaged", Venue.class
    ) ;
    final Choice choice = new Choice(client) ;
    for (Venue near : toRepair) {
      final boolean needsRepair =
        near.structure.hasWear() ||
        near.structure.burning() ||
        near.structure.needsUpgrade() ||
        near.structure.needsSalvage() ;
      if (! needsRepair) continue ;
      final Building b = new Building(client, near) ;
      b.priorityMod = priorityMod ;
      choice.add(b) ;
    }
    
    return (Building) choice.weightedPick(0) ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  public boolean finished() {
    if (super.finished() || built.base().credits() <= 0) return true ;
    if (built.structure.hasWear()) return false ;
    if (built.structure.needsUpgrade()) return false ;
    return true ;
  }
  
  
  protected Behaviour getNextStep() {
    if (built.base().credits() <= 0) return null ;
    if (built.structure.needsUpgrade() && built.structure.goodCondition()) {
      final Action upgrades = new Action(
        actor, built,
        this, "actionUpgrade",
        Action.BUILD, "Upgrading "+built
      ) ;
      return upgrades ;
    }
    if (built.structure.hasWear()) {
      final Action building = new Action(
        actor, built,
        this, "actionBuild",
        Action.BUILD, "Assembling "+built
      ) ;
      if (! Spacing.adjacent(actor.origin(), built) || Rand.num() < 0.2f) {
        final Tile t = Spacing.pickFreeTileAround(built, actor) ;
        if (t == null) {
          abortBehaviour() ;
          return null ;
        }
        building.setMoveTarget(t) ;
      }
      else building.setMoveTarget(actor.origin()) ;
      return building ;
    }
    return null ;
  }
  
  
  public boolean actionBuild(Actor actor, Venue built) {
    //
    //  Double the rate of repair again if you have proper tools and materials.
    final boolean salvage = built.structure.needsSalvage() ;
    int success = actor.traits.test(HARD_LABOUR, ROUTINE_DC, 1) ? 1 : 0 ;
    //
    //  TODO:  Base assembly DC (or other skills) on a Conversion for the
    //  structure.  Require construction materials for full efficiency.
    if (salvage) {
      success *= actor.traits.test(ASSEMBLY, 5, 1) ? 2 : 1 ;
      final float amount = 0 - built.structure.repairBy(success * 5f / -2) ;
      final float cost = amount * built.structure.buildCost() ;
      built.stocks.incCredits(cost * 0.5f) ;
    }
    else {
      success *= actor.traits.test(ASSEMBLY, 10, 0.5f) ? 2 : 1 ;
      success *= actor.traits.test(ASSEMBLY, 20, 0.5f) ? 2 : 1 ;
      final boolean intact = built.structure.intact() ;
      final float amount = built.structure.repairBy(success * 5f / 2) ;
      final float cost = amount * built.structure.buildCost() ;
      built.stocks.incCredits((0 - cost) * (intact ? 0.5f : 1)) ;
    }
    return true ;
  }
  
  
  public boolean actionUpgrade(Actor actor, Venue built) {
    final Upgrade upgrade = built.structure.upgradeInProgress() ;
    ///if (upgrade == null) I.say("NO UPGRADE!") ;
    if (upgrade == null) return false ;
    ///I.say("Advancing upgrade: "+upgrade.name) ;
    int success = 1 ;
    success *= actor.traits.test(ASSEMBLY, 10, 0.5f) ? 2 : 1 ;
    success *= actor.traits.test(ASSEMBLY, 20, 0.5f) ? 2 : 1 ;
    final float amount = built.structure.advanceUpgrade(success * 1f / 100) ;
    final float cost = amount * upgrade.buildCost ;
    built.base().incCredits((0 - cost)) ;
    return true ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    d.append("Repairing ") ;
    d.append(built) ;
  }
}








