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



//
//  Merge this with the hunting class.  No- have this as a subset of the
//  hunting class!


public class Combat extends Plan implements ActorConstants {
  
  
  /**  
    */
  public static interface Participant { CombatStats combatStats() ; }
  
  //  Also allow Venues as targets.
  //final Actor target ;
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
    //
    //  TODO:  Move this evaluation below, to the combatPriority method?
    if (target instanceof Actor) {
      final Actor struck = (Actor) target ;
      float relation = actor.AI.relation(struck) ;
      relation *= PARAMOUNT ;
      if (actor.base() == struck.base()) relation += ROUTINE ;
      return combatPriority(actor, struck, 0 - relation, PARAMOUNT) ;
    }
    if (target instanceof Venue) {
      final Venue struck = (Venue) target ;
      return 0 - actor.AI.relation(struck.base()) * ROUTINE ;
    }
    return -1 ;
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
    //  TODO:  Retrofit this to work with buildings?  What about offensive
    //  structures?
    
    if (actor == enemy || winReward <= 0) return 0 ;
    final float
      actorStrength = combatStrength(actor),
      enemyStrength = combatStrength(enemy),
      chance = actorStrength / (actorStrength + enemyStrength) ;
    I.say(
      "Actor/enemy strength, chance: "+actorStrength+"/"+enemyStrength+
      ", "+chance
    ) ;
    float appeal = 0 ;
    appeal += winReward * chance ;
    appeal -= (1 - chance) * lossCost ;
    I.say("Final appeal: "+appeal) ;
    
    //
    //final float distance = Spacing.distance(enemy, actor) ;
    //  You also need to incorporate an estimate of dangers en-route,
    //  and subtract this (relative to the actor's combat strength.)
    //...Some of this should be moved to the general Mission class.
    //appeal /= Math.max(1, distance / Terrain.SECTOR_SIZE) ;
    return appeal ;
  }
  
  
  //  TODO:  Actors may need to cache this value?  Maybe later.  Not urgent at
  //  the moment.
  public static float combatStrength(Actor actor) {
    float strength = 0 ;
    strength += (actor.gear.armourRating() + actor.gear.attackDamage()) / 20 ;
    strength *= actor.health.maxHealth() / 10 ;
    //strength *= actor.traits.trueLevel(REFLEX) / 10 ;
    strength *= (1 - actor.health.injuryLevel()) ;
    strength *= 1 - actor.health.skillPenalty() ;
    //
    //  You might need to include modifiers for ranged and melee attack skill,
    //  plus reflex and shields.
    //  Work this out later.  Different armour/weapon combos might be relevant.
    /*
    if (enemy != null) {
      if (enemy.gear.meleeWeapon()) {
        strength *= enemy.traits.chance(CLOSE_COMBAT, actor, CLOSE_COMBAT, 0) ;
      }
      else {
        strength *= enemy.traits.chance(, actor, MARKSMANSHIP, 0) ;
      }
    }
    //*/
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
      actor.origin(), actor, actor.AI.seen()
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
    if (melee) {
      strike.setProperties(Action.QUICK) ;
      if (razes && (Rand.num() < 0.2f || ! Spacing.adjacent(actor, target))) {
        strike.setMoveTarget(Spacing.pickFreeTileAround(target, actor)) ;
      }
    }
    else {
      final float range = actor.health.sightRange() ;
      if (Rand.num() < danger) {
        final Tile WP = Retreat.pickWithdrawPoint(actor, range, target, 0.1f) ;
        strike.setMoveTarget(WP) ;
        strike.setProperties(Action.QUICK) ;
      }
      else if (razes && Rand.num() < 0.2f) {
        final Tile alt = Spacing.pickRandomTile(actor, 3) ;
        if (Spacing.distance(alt, target) < range && alt.owner() == null) {
          strike.setMoveTarget(alt) ;
        }
        strike.setProperties(Action.QUICK) ;
      }
      else strike.setProperties(Action.RANGED | Action.QUICK) ;
    }
    return strike ;
  }
  
  
  public boolean actionStrike(Actor actor, Actor target) {
    if (target.health.deceased()) return false ;
    //
    //  You may want a separate category for animals.
    if (actor.gear.meleeWeapon()) {
      performStrike(actor, target, CLOSE_COMBAT, CLOSE_COMBAT) ;
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
      offence, target, defence, 0 - rangePenalty(actor, target), 10
    ) ;
    if (success) {
      float damage = actor.gear.attackDamage() * Rand.num() * 2 ;
      damage -= target.gear.armourRating() * Rand.num() * 2 ;
      if (damage > 0) target.health.takeInjury(damage) ;
    }
    DeviceType.applyFX(actor.gear.deviceType(), actor, target, success) ;
  }
  
  
  static void performSiege(
    Actor actor, Venue besieged
  ) {
    boolean accurate = false ;
    if (actor.gear.meleeWeapon()) {
      accurate = actor.traits.test(CLOSE_COMBAT, 0, 1) ;
    }
    else {
      final float penalty = rangePenalty(actor, besieged) ;
      accurate = actor.traits.test(MARKSMANSHIP, penalty, 1) ;
    }
    
    float damage = actor.gear.attackDamage() * Rand.num() * 2 ;
    if (accurate) damage *= 1.5f ;
    else damage *= 0.5f ;
    damage -= besieged.structure.armouring() * Rand.num() * 2 ;
    
    if (damage > 0) besieged.structure.takeDamage(damage) ;
    DeviceType.applyFX(actor.gear.deviceType(), actor, besieged, true) ;
  }
  
  
  static float rangePenalty(Actor a, Target t) {
    final float range = Spacing.distance(a, t) ;
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