/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.tactical ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.social.* ;
import src.user.* ;
import src.util.* ;



public class Combat extends Plan implements ActorConstants {
  
  
  /**  
    */
  static boolean verbose = false ;
  final Element target ;
  
  
  public Combat(Actor actor, Element target) {
    super(actor, target) ;
    this.target = target ;
  }
  
  
  public Combat(Session s) throws Exception {
    super(s) ;
    this.target = (Element) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(target) ;
  }
  
  
  
  
  
  
  
  /**  Gauging the relative strength of combatants, odds of success, and how
    *  (un)appealing an engagement would be.
    */
  public float priorityFor(Actor actor) {
    if (isDead(target)) return 0 ;
    if (target instanceof Actor) {
      final Actor struck = (Actor) target ;
      float BP = combatPriority(actor, struck, priorityMod, PARAMOUNT) ;
      return BP <= 0 ? 0 : BP + ROUTINE ;
    }
    if (target instanceof Venue) {
      final Venue struck = (Venue) target ;
      float BP = (priorityMod - actor.mind.relation(struck.base())) * ROUTINE ;
      return BP <= 0 ? 0 : BP + ROUTINE ;
    }
    return -1 ;
  }
  
  
  public boolean valid() {
    if (target instanceof Actor && ((Actor) target).indoors()) return false ;
    return super.valid() ;
  }
  
  
  public static boolean isDead(Element subject) {
    if (subject instanceof Actor)
      return ((Actor) subject).health.deceased() ;
    if (subject instanceof Venue)
      return ((Venue) subject).structure.destroyed() ;
    return false ;
  }
  
  
  public static float combatPriority(
    Actor actor, Actor enemy, float winReward, float lossCost
  ) {
    if (actor == enemy) return 0 ;
    //
    //  Here, we estimate the danger presented by actors in the area, and
    //  thereby guage the odds of survival/victory-
    final Batch <Element> nearby = new Batch <Element> () ;
    //
    //  TODO:  Strictly speaking, this is cheating, since some of these targets
    //  might not be visible to the player (or actor.)  Fix that?
    for (Object m : actor.world().presences.matchesNear(
      Mobile.class, enemy, actor.health.sightRange())
    ) {
      nearby.add((Element) m) ;
    }
    float danger = Retreat.dangerAtSpot(enemy, actor, nearby) ;
    //
    /*  //TODO:  Fix up the Danger Map.  Needs to be smoother, area-wise.
    if (actor.base() != null) {
      final float areaDanger = actor.base().dangerMap.valAt(enemy.origin()) ;
      danger = (danger + (areaDanger / combatStrength(actor, null))) / 2 ;
    }
    //*/
    final float chance = 1 - danger ;
    //
    //  Then we calculate the risk/reward ratio associated with the act-
    final Target victim = enemy.targetFor(Combat.class) ;
    if (victim instanceof Actor) {
      float mod = alliance(actor, (Actor) victim) * PARAMOUNT ;
      lossCost -= mod / 2 ;
      winReward += mod / 2 ;
    }
    float appeal = 0 ;
    final float ER = alliance(actor, enemy) ;
    appeal += ER * -1 * PARAMOUNT ;
    appeal += winReward ;
    
    
    final boolean reports = verbose && BaseUI.isPicked(actor) ;
    if (reports) {
      I.say(
        "  "+actor+" considering COMBAT with "+enemy+
        ", time: "+actor.world().currentTime()
      ) ;
      I.say(
        "  Danger level: "+danger+", relation: "+ER+
        "\n  Appeal before chance: "+appeal+", chance: "+chance
      ) ;
    }
    if (chance <= 0) {
      if (reports) I.say("  No chance of victory!\n") ;
      return 0 ;
    }
    appeal = (appeal * chance) - ((1 - chance) * lossCost) ;
    if (reports) I.say("  Final appeal: "+appeal+"\n") ;
    return appeal ;
  }
  
  
  public static float alliance(Actor a, Actor b) {
    //
    //  TODO:  Try averaging the two instead?  Or just base on one?  Replace
    //  with a threatFrom() method, that includes things like retreat status,
    //  et cetera?  ...Yes.  Factor it out.
    return Math.min(a.mind.relation(b), b.mind.relation(a)) ;
  }
  
  
  //
  //  TODO:  Actors may need to cache this value?  Maybe later.  Not urgent at
  //  the moment.
  //
  //  Note:  it's acceptable to pass null as the enemy argument, for a general
  //  estimate of combat prowess.  (TODO:  Put in a separate method for that?)
  public static float combatStrength(Actor actor, Actor enemy) {
    float strength = 0 ;
    strength += (actor.gear.armourRating() + actor.gear.attackDamage()) / 20f ;
    strength *= actor.health.maxHealth() / 10 ;
    strength *= (1 - actor.health.injuryLevel()) ;
    strength *= 1 - actor.health.stressPenalty() ;
    //
    //
    if (enemy == null) {
      strength *= (
        actor.traits.useLevel(HAND_TO_HAND) +
        actor.traits.useLevel(MARKSMANSHIP)
      ) / 20 ;
      strength *= (
        actor.traits.useLevel(HAND_TO_HAND) +
        actor.traits.useLevel(STEALTH_AND_COVER)
      ) / 20 ;
    }
    else {
      final Skill attack, defend ;
      if (actor.gear.meleeWeapon()) {
        attack = defend = HAND_TO_HAND ;
      }
      else {
        attack = MARKSMANSHIP ;
        defend = STEALTH_AND_COVER ;
      }
      final float chance = actor.traits.chance(attack, enemy, defend, 0) ;
      strength *= 2 * chance ;
    }
    return strength ;
  }
  
  
  
  
  /**  Actual behaviour implementation-
    */
  protected Behaviour getNextStep() {
    //
    //  This might need to be tweaked in cases of self-defence, where you just
    //  want to see off an attacker.
    if (isDead(target)) return null ;
    Action strike = null ;
    final boolean melee = actor.gear.meleeWeapon() ;
    final boolean razes = target instanceof Venue ;
    final float danger = Retreat.dangerAtSpot(
      actor.origin(), actor, actor.mind.seen()
    ) ;
    
    final String strikeAnim = melee ?
      Action.STRIKE :
      Action.FIRE ;
    if (razes) {
      strike = new Action(
        actor, target,
        this, "actionSiege",
        strikeAnim, "Razing "+target
      ) ;
    }
    else {
      strike = new Action(
        actor, target,
        this, "actionStrike",
        strikeAnim, "Striking at "+target
      ) ;
    }
    //
    //  Depending on the type of target, and how dangerous the area is, a bit
    //  of dancing around may be in order.
    if (melee) configMeleeAction(strike, razes, danger) ;
    else configRangedAction(strike, razes, danger) ;
    return strike ;
  }
  
  
  private void configMeleeAction(Action strike, boolean razes, float danger) {
    final World world = actor.world() ;
    strike.setProperties(Action.QUICK) ;
    if (razes) {
      if (! Spacing.adjacent(actor, target)) {
        strike.setMoveTarget(Spacing.nearestOpenTile(target, actor, world)) ;
      }
      else if (Rand.num() < 0.2f) {
        strike.setMoveTarget(Spacing.pickFreeTileAround(target, actor)) ;
      }
      else strike.setMoveTarget(actor.origin()) ;
    }
  }
  
  
  private void configRangedAction(Action strike, boolean razes, float danger) {
    final float range = actor.health.sightRange() ;
    boolean underFire = actor.world().activities.includes(actor, Combat.class) ;
    boolean tooFar = Spacing.distance(actor, target) > range ;
    if ((Rand.num() < danger && underFire) || tooFar) {
      final Tile WP = Retreat.pickWithdrawPoint(actor, range, target, 0.1f) ;
      strike.setMoveTarget(WP) ;
      strike.setProperties(Action.QUICK) ;
    }
    else if (razes && Rand.num() < 0.2f) {
      final Tile alt = Spacing.pickRandomTile(actor, 3, actor.world()) ;
      if ((Spacing.distance(alt, target) < range) && alt.owner() == null) {
        strike.setMoveTarget(alt) ;
        strike.setProperties(Action.QUICK) ;
      }
      else strike.setProperties(Action.RANGED | Action.QUICK) ;
    }
    else {
      strike.setProperties(Action.RANGED | Action.QUICK) ;
    }
  }
  
  
  
  /**  Executing the action-
    */
  public boolean actionStrike(Actor actor, Actor target) {
    if (target.health.deceased()) return false ;
    //
    //  You may want a separate category for animals.
    if (actor.gear.meleeWeapon()) {
      performStrike(actor, target, HAND_TO_HAND, HAND_TO_HAND) ;
    }
    else {
      performStrike(actor, target, MARKSMANSHIP, STEALTH_AND_COVER) ;
    }
    return true ;
  }
  
  
  public boolean actionSiege(Actor actor, Venue target) {
    if (target.structure.destroyed()) return false ;
    performSiege(actor, target) ;
    return true ;
  }
  
  
  //
  //  You may also need to decrement shields.
  static void performStrike(
    Actor actor, Actor target,
    Skill offence, Skill defence
  ) {
    final boolean success = actor.traits.test(
      offence, target, defence, 0 - rangePenalty(actor, target), 1
    ) ;
    if (success) {
      float damage = actor.gear.attackDamage() * Rand.avgNums(2) ;
      damage -= target.gear.armourRating() * Rand.avgNums(2) ;
      if (damage > 0) target.health.takeInjury(damage) ;
    }
    DeviceType.applyFX(actor.gear.deviceType(), actor, target, success) ;
  }
  
  
  static void performSiege(
    Actor actor, Venue besieged
  ) {
    boolean accurate = false ;
    if (actor.gear.meleeWeapon()) {
      accurate = actor.traits.test(HAND_TO_HAND, 0, 1) ;
    }
    else {
      final float penalty = rangePenalty(actor, besieged) ;
      accurate = actor.traits.test(MARKSMANSHIP, penalty, 1) ;
    }
    
    float damage = actor.gear.attackDamage() * Rand.avgNums(2) * 1.5f ;
    if (accurate) damage *= 1.5f ;
    else damage *= 0.5f ;
    
    final float armour = besieged.structure.armouring() ;
    damage -= armour * (Rand.avgNums(2) + 0.25f) ;
    damage *= 5f / (5 + armour) ;
    
    I.say("Armour/Damage: "+armour+"/"+damage) ;
    
    if (damage > 0) besieged.structure.takeDamage(damage) ;
    DeviceType.applyFX(actor.gear.deviceType(), actor, besieged, true) ;
  }
  
  
  static float rangePenalty(Actor a, Target t) {
    final float range = Spacing.distance(a, t) / 2 ;
    return range * 5 / (a.health.sightRange() + 1f) ;
  }
  
  
  public void describeBehaviour(Description d) {
    d.append("In combat with ") ;
    d.append(target) ;
  }
}




/*
  public static boolean strikeCheck(
    Actor actor, Actor opponent, boolean offensive
  ) {
    final SkillType strikeSkill, defendSkill ;
    final int defendMove ;
    //
    //  TODO:  If the opponent has no melee weapon, they can't use close combat
    //  to defend themselves.
    float penalty = 0, bonus = 0 ;
    if (actor.equipment.meleeWeapon()) {
      strikeSkill = Vocation.CLOSE_COMBAT ;
      defendSkill = Vocation.CLOSE_COMBAT ;
      defendMove = MOVE_BLOCK ;
      bonus += foesPenalty(opponent) ;
    }
    else {
      strikeSkill = Vocation.MARKSMANSHIP ;
      defendSkill = Vocation.EVASION ;
      defendMove = MOVE_DODGE ;
      penalty += coverBonus(actor, actor, opponent) ;
    }
    if (offensive) bonus += timingPenalty(opponent, actor) ;
    if (moveFor(opponent) == defendMove) penalty += 5 ;
    //
    //  If the opponent is not focused on combat, or on the actor, then the
    //  check becomes much easier (TODO:  Implement that.)
    final boolean success = actor.training.skillTest(
      strikeSkill, bonus, STRIKE_XP,
      defendSkill, opponent, penalty
    ) ;
    return success ;
  }
  
  
  public static boolean dealDamage(
    Actor actor, Target toStrike, boolean critical
  ) {
    if (toStrike instanceof Actor) {
      final Actor foe = (Actor) toStrike ;
      final boolean
        melee = actor.equipment.meleeWeapon(),
        physical = actor.equipment.physicalWeapon() ;
      float damage = actor.equipment.attackDamage() * Rand.avgNums(2) ;
      if (! melee) {
        damage = foe.equipment.afterShields(actor, damage, physical) ;
      }
      if (critical) {
        damage *= MIN_HITS + (Rand.num() * MAX_HITS) ;
      }
      damage = foe.equipment.afterArmour(actor, damage, physical) ;
      if (damage <= 0) return false ;
      foe.health.takeInjury(damage) ;
      return true ;
    }
    if (toStrike instanceof Venue) {
      final Venue besieged = (Venue) toStrike ;
      float damage = actor.equipment.attackDamage() * Rand.avgNums(2) ;
      if (critical) damage *= MIN_HITS + (Rand.num() * MAX_HITS) ;
      besieged.integrity.takeDamage(damage) ;
      return true ;
    }
    return false ;
  }
  
  
//*/