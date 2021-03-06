/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.debug ;
import src.game.actors.* ;
import src.game.base.* ;
import src.game.building.* ;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.tactical.* ;
import src.game.wild.* ;
import src.graphics.common.* ;
import src.graphics.widgets.* ;
import src.user.* ;
import src.util.* ;


//  Cantina.  Archives.  Audit Office.  Vault System.  Holdings.  Air Processor.
//
//  Update farming/the vats/mining a bit (including minimum spacing?)
//  A clearer factoring out of venue/actor batches in the AI.
//  Have pollution effects impact health/life-support, and possibly change the
//  landscape.
//
//  Micovores are retreating from prey too easily!  Find out why!
//  
//  Actors need to be automatically aware of persons attacking them, should
//  call for help from allies, and need proper line of sight.  Add Security and
//  Contact missions.  Use the threatLevel() function for both retreat and
//  attack.  Have missions modify choice priorities.
//
//  Add water and life support from biomass and other buildings.
//
//  Tweak mechanics for diplomacy and citizen mood.  (Good relations are way
//  too easy/quick at the moment.)
//
//  Simplify the user interface, implement Powers, and add a Main Menu.  That's
//  it.


public class DebugBehaviour extends PlayLoop implements BuildConstants {
  
  
  
  /**  Startup and save/load methods-
    */
  public static void main(String args[]) {
    DebugBehaviour test = new DebugBehaviour() ;
    PlayLoop.runLoop(test) ;
  }
  
  
  protected DebugBehaviour() {
    super() ;
  }
  
  
  public DebugBehaviour(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Setup and updates-
    */
  protected boolean shouldExitLoop() {
    if (KeyInput.wasKeyPressed('r')) {
      resetGame() ;
      return false ;
    }
    if (KeyInput.wasKeyPressed('f')) {
      I.say("Paused? "+PlayLoop.paused()) ;
      PlayLoop.setPaused(! PlayLoop.paused()) ;
    }
    if (KeyInput.wasKeyPressed('s')) {
      I.say("SAVING GAME...") ;
      PlayLoop.saveGame("saves/test_session.rep") ;
      return false ;
    }
    if (KeyInput.wasKeyPressed('l')) {
      I.say("LOADING GAME...") ;
      //GameSettings.frozen = true ;
      PlayLoop.loadGame("saves/test_session.rep") ;
      return true ;
    }
    return false ;
  }
  
  
  protected World createWorld() {
    final TerrainGen TG = new TerrainGen(
      32, 0.2f,
      Habitat.MEADOW , 0.7f,
      Habitat.BARRENS, 0.3f
    ) ;
    final World world = new World(TG.generateTerrain()) ;
    TG.setupMinerals(world, 0, 0, 0) ;
    //TG.setupOutcrops(world) ;
    return world ;
  }
  
  
  protected Base createBase(World world) {
    Base base = new Base(world) ;
    return base ;
  }
  
  
  protected HUD createUI(Base base, Rendering rendering) {
    BaseUI UI = new BaseUI(base.world, rendering) ;
    UI.assignBaseSetup(base, new Vec3D(8, 8, 0)) ;
    return UI ;
  }
  
  
  protected boolean loadedAtStartup() {
    try {
      PlayLoop.loadGame("saves/test_session.rep") ;
      final Base base = PlayLoop.played() ;
      if (base.credits() < 2000) base.incCredits(2000) ;
      PlayLoop.setGameSpeed(1.0f) ;
      return true ;
    }
    catch (Exception e) { I.report(e) ; return false ; }
  }
  
  
  protected void finalize() throws Throwable {
    I.say(this+" BEING GARBAGE COLLECTED!") ;
    super.finalize() ;
  }


  protected void configureScenario(World world, Base base, HUD HUD) {
    //natureScenario(world, base, HUD) ;
    baseScenario(world, base, HUD) ;
    //missionScenario(world, base, HUD) ;
    //socialScenario(world, base, HUD) ;
  }
  
  
  protected void renderGameGraphics() {
    super.renderGameGraphics() ;
    if (((BaseUI) currentUI()).currentTask() == null) {
      //DebugPathing.highlightPlace() ;
      //DebugPathing.highlightPath() ;
    }
  }
  

  protected void updateGameState() {
    super.updateGameState() ;
  }
  
  
  
  /**  Testing out interactions between alien creatures or primitive humanoids.
    */
  private void natureScenario(World world, Base base, HUD UI) {
    GameSettings.noFog = true ;
    PlayLoop.setGameSpeed(5.0f) ;
    
    final EcologyGen EG = new EcologyGen() ;
    EG.populateFlora(world) ;
    
    final Actor hunter = new Micovore() ;
    hunter.health.setupHealth(0.5f, 1, 0) ;
    hunter.enterWorldAt(6, 6, world) ;
    ((BaseUI) UI).selection.pushSelection(hunter, true) ;
    
    for (int n = 16 ; n-- > 0 ;) {
      final Actor prey = Rand.yes() ? new Quud() : new Vareen() ;
      final Tile e = Spacing.pickRandomTile(world.tileAt(16, 16), 8, world) ;
      prey.health.setupHealth(Rand.num(), 1, 0) ;
      prey.enterWorldAt(e.x, e.y, world) ;
    }
  }
  
  
  
  /**  These are scenarios associated with upkeep, maintenance and
    *  construction of the settlement-
    */
  private void baseScenario(World world, Base base, HUD UI) {
    
    ///PlayLoop.rendering().port.cameraZoom = 1.33f ;
    base.incCredits(2000) ;
    base.commerce.assignHomeworld(Background.PLANET_HALIBAN) ;
    final Bastion bastion = new Bastion(base) ;
    
    //
    //  TODO:  Aristocrats like these might need different behavioural routines.
    final Human knight = new Human(Background.KNIGHTED, base) ;
    Human spouse = null ;
    float bestRating = Float.NEGATIVE_INFINITY ;
    for (int n = 10 ; n-- > 0 ;) {
      final Human match = new Human(Background.CONSORT, base) ;
      final float rating =
        knight.mind.attraction(match) +
        (match.mind.attraction(knight) / 2) ;
      if (rating > bestRating) { spouse = match ; bestRating = rating ; }
    }
    
    knight.mind.setHomeVenue(bastion) ;
    spouse.mind.setHomeVenue(bastion) ;
    final int initTime = 0 - Rand.index(100) ;
    final float relation = bestRating * Rand.avgNums(2) ;
    knight.mind.setRelation(spouse, relation, initTime) ;
    spouse.mind.setRelation(knight, relation, initTime) ;
    
    base.assignRuler(knight) ;
    establishVenue(bastion, 9, 9, true, knight, spouse) ;
    ((BaseUI) UI).selection.pushSelection(knight, true) ;
    
    establishVenue(new SupplyDepot(base), 20, 10, true) ;
  }
  
  
  
  /**  Testing out directed behaviour like combat, exploration, security or
    *  contact missions.
    */
  private void missionScenario(World world, Base base, HUD UI) {
    
    GameSettings.noFog = true ;
    //GameSettings.hireFree = true ;
    PlayLoop.setGameSpeed(1.0f) ;
    
    final Base otherBase = new Base(world) ;
    world.registerBase(otherBase, true) ;
    base.setRelation(otherBase, -1) ;
    otherBase.setRelation(base, -1) ;
    otherBase.colour = Colour.CYAN ;
    
    final Batch <Actor> allies = new Batch <Actor> () ;
    float sumPower = 0 ;
    for (int n = 5 ; n-- > 0 ;) {
      final Actor actor = new Human(Background.EXPLORER, base) ;
      actor.setPosition(
        24 + Rand.range(-4, 4),
        24 + Rand.range(-4, 4),
        world
      ) ;
      actor.enterWorld() ;
      allies.add(actor) ;
      sumPower += Combat.combatStrength(actor, null) ;
      ///establishVenue(new SurveyorRedoubt(base), 4, 4, true, actor) ;
    }
    ///I.say("TOTAL POWER OF ALLIES: "+sumPower) ;
    
    //*
    final EcologyGen EG = new EcologyGen() ;
    final Batch <Ruins> ruins = EG.populateRuins(world.tileAt(8, 8), 16) ;
    EG.populateArtilects(ruins, world) ;
    EG.populateFlora(world) ;
    //*/
    
    /*
    final Actor enemy = new Tripod() ;
    //enemy.assignBase(otherBase) ;
    enemy.enterWorldAt(24, 24, world) ;
    ((BaseUI) UI).selection.pushSelection(enemy, true) ;
    
    I.say("POWER OF ENEMY: "+Combat.combatStrength(enemy, null)) ;
    //*/
    //((BaseUI) UI).selection.pushSelection(allies.atIndex(0), true) ;
  }
  
  
  
  /**  Testing out pro-social behaviour like dialogue, recreation and medical
    *  treatment.
    */
  private void socialScenario(final World world, Base base, HUD UI) {
    base.incCredits(1000) ;
    GameSettings.noFog = true ;
    GameSettings.buildFree = true ;
    
    final Actor actor = new Human(Background.PHYSICIAN, base) ;
    final Actor other = new Human(Background.VETERAN, base) ;
    other.health.takeInjury(other.health.maxHealth()) ;
    ((BaseUI) UI).selection.pushSelection(actor, true) ;
    
    actor.enterWorldAt(4, 4, world) ;
    other.enterWorldAt(6, 6, world) ;
    establishVenue(new Sickbay(base), 9, 2, true, actor) ;
    /*
    establishVenue(new Garrison(base), 2, 9, true, other) ;
    establishVenue(new Cantina(base), 9, 9, true) ;
    //*/
    
    final EcologyGen EG = new EcologyGen() ;
    //EG.populateFlora(world) ;
    ///EG.populateFauna(world, Species.VAREEN) ;
  }
  
  
  public static Venue establishVenue(
    Venue v, int atX, int atY, boolean intact,
    Actor... employed
  ) {
    final World world = PlayLoop.world() ;
    v.enterWorldAt(atX, atY, world) ;
    if (intact) {
      v.structure.setState(Structure.STATE_INTACT, 1.0f) ;
      v.onCompletion() ;
    }
    else {
      v.structure.setState(Structure.STATE_INSTALL, 0.0f) ;
    }
    final Tile e = world.tileAt(v) ;
    for (Actor a : employed) {
      a.mind.setEmployer(v) ;
      if (! a.inWorld()) {
        a.enterWorldAt(e.x, e.y, world) ;
        a.goAboard(v, world) ;
      }
    }
    VenuePersonnel.fillVacancies(v) ;
    v.setAsEstablished(true) ;
    return v ;
  }
}





/*
final Actor actorA = new Human(Vocation.RUNNER, base) ;
actorA.enterWorldAt(15, 15, world) ;
final Actor actorB = new Human(Vocation.VETERAN, base) ;
actorB.enterWorldAt(15, 3, world) ;

final Actor target = new Quud() ;
target.health.setupHealth(0.5f, 1, 0) ;
target.enterWorldAt(5, 5, world) ;

final Base otherBase = new Base(world) ;
world.registerBase(otherBase, true) ;
base.setRelation(otherBase, -1) ;
otherBase.setRelation(base, -1) ;

final Venue garrison = new Garrison(otherBase) ;
establishVenue(garrison, 8, 8, true) ;
actorA.AI.assignBehaviour(new Combat(actorA, garrison)) ;
actorB.AI.assignBehaviour(new Combat(actorA, garrison)) ;
((BaseUI) UI).selection.pushSelection(actorA, true) ;
//*/

/*
final Mission mission = new ReconMission(base, world.tileAt(20, 20)) ;
base.addMission(mission) ;
((BaseUI) UI).selection.setSelected(mission) ;
((BaseUI) UI).camera.zoomNow(mission.subject()) ;
//*/
/*
final Mission mission = new StrikeMission(base, garrison) ;
mission.setApplicant(assails, true) ;
base.addMission(mission) ;
((BaseUI) UI).selection.setSelected(mission) ;
//*/


