/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.base ;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;


//
//  TODO:  Get rid of the Petrocarbs production from forestry, since the Air
//  Processor already has that covered.  (That and mining of course.)

//
//  Have research take longer, deliver samples instead of gene seed, and top
//  up reserves of each crop individually.



public class BotanicalStation extends Venue implements BuildConstants {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final static String IMG_DIR = "media/Buildings/ecologist/" ;
  final static Model
    STATION_MODEL = ImageModel.asIsometricModel(
      BotanicalStation.class, IMG_DIR+"botanical_station.png", 4, 3
    ) ;
  
  
  
  final static int MAX_PLANT_RANGE = 16 ;
  
  final List <Plantation> allotments = new List <Plantation> () ;
  
  
  
  public BotanicalStation(Base belongs) {
    super(4, 3, Venue.ENTRANCE_SOUTH, belongs) ;
    structure.setupStats(
      150, 3, 250,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    ) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    attachSprite(STATION_MODEL.makeSprite()) ;
  }
  
  
  public BotanicalStation(Session s) throws Exception {
    super(s) ;
    s.loadObjects(allotments) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObjects(allotments) ;
  }
  
  
  
  /**  Handling upgrades and economic functions-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    BotanicalStation.class, "botanical_upgrades"
  ) ;
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    CEREAL_LAB = new Upgrade(
      "Cereal Lab",
      "Improves cereal yields.  Cereals yield more calories than other crop "+
      "species, but lack the full range of nutrients required in a healthy "+
      "diet.",
      100,
      CARBS, 1,
      null, ALL_UPGRADES
    ),
    BROADLEAF_LAB = new Upgrade(
      "Broadleaf Lab",
      "Improves broadleaf yields.  Broadleaves provide a wider range of "+
      "nutrients, and are valued as luxury exports, but their yield is small.",
      150,
      GREENS, 1,
      null, ALL_UPGRADES
    ),
    FIELD_HAND_STATION = new Upgrade(
      "Field Hand Station",
      "Hire additional field hands to plant and reap the harvest more "+
      "quickly, maintain equipment, and bring land under cultivation.",
      50,
      Background.FIELD_HAND, 1,
      null, ALL_UPGRADES
    ),
    TREE_FARMING = new Upgrade(
      "Tree Farming",
      "Forestry programs assist in terraforming efforts and climate "+
      "moderation, as well as providing carbons for plastic production.",
      100,
      PETROCARBS, 1,
      BROADLEAF_LAB, ALL_UPGRADES
    ),
    INSECTRY_LAB = new Upgrade(
      "Insectry Lab",
      "Many plantations cultivate colonies of social insects or other "+
      "invertebrates, both as a source of protein and pollination, pest "+
      "control, or recycling services.",
      150,
      PROTEIN, 1,
      BROADLEAF_LAB, ALL_UPGRADES
    ),
    ECOLOGIST_STATION = new Upgrade(
      "Ecologist Station",
      "Ecologists are highly-skilled students of plants, animals and gene "+
      "modification, capable of adapting species to local climate conditions.",
      150,
      Background.ECOLOGIST, 1,
      TREE_FARMING, ALL_UPGRADES
    ) ;
  
  
  public Behaviour jobFor(Actor actor) {
    if (! structure.intact() || Planet.isNight(world)) return null ;
    //
    //  If the harvest is really coming in, pitch in regardless-
    final Choice choice = new Choice(actor) ;
    final boolean needsSeed = stocks.amountOf(GENE_SEED) < 5 ;
    for (Plantation p : allotments) {
      if (p.needForTending() > 0.5f) choice.add(new Farming(actor, p)) ;
    }
    if (choice.size() > 0) return choice.weightedPick(0) ;
    //
    //  Otherwise, perform deliveries and more casual work-
    if (! personnel.onShift(actor)) return null ;
    final Delivery d = Deliveries.nextDeliveryFor(
      actor, this, services(), 10, world
    ) ;
    choice.add(d) ;
    //
    //  Forestry may have to be performed, depending on need for gene samples-
    final Forestry f = new Forestry(actor, this) ;
    if (needsSeed && actor.vocation() == Background.ECOLOGIST) {
      f.priorityMod += Plan.ROUTINE ;
      f.configureFor(Forestry.STAGE_SAMPLING) ;
    }
    else {
      f.priorityMod = structure.upgradeLevel(TREE_FARMING) ;
      f.configureFor(Forestry.STAGE_GET_SEED) ;
    }
    choice.add(f) ;
    //
    //  And lower-priority tending and upkeep also gets an appearance-
    choice.add(researchAction(actor)) ;
    for (Plantation p : allotments) choice.add(new Farming(actor, p)) ;
    return choice.weightedPick(0) ;
  }
  
  
  protected Action researchAction(Actor actor) {
    if (actor.vocation() != Background.ECOLOGIST) return null ;
    if (stocks.amountOf(GENE_SEED) <= 0) return null ;
    Plantation needs = null ;
    for (Plantation p : allotments) if (p.type == Plantation.TYPE_NURSERY) {
      if (p.stocks.amountOf(GENE_SEED) < 0.5f) needs = p ;
    }
    if (needs == null) return null ;
    if (actor.gear.amountOf(GENE_SEED) > 0.5f) {
      final Action deliver = new Action(
        actor, needs,
        this, "actionDeliverSeed",
        Action.STAND, "Delivering seed"
      ) ;
      return deliver ;
    }
    else {
      final Action prepare = new Action(
        actor, this,
        this, "actionPrepareSeed",
        Action.STAND, "Preparing seed"
      ) ;
      return prepare ;
    }
  }
  
  
  public boolean actionPrepareSeed(Actor actor, BotanicalStation lab) {
    //
    //  Calculate odds of success based on the skill of the researcher-
    final float successChance = 0.1f ;
    float skillRating = 5 ;
    if (! actor.traits.test(GENE_CULTURE, ROUTINE_DC, 5.0f)) skillRating /= 2 ;
    if (! actor.traits.test(CULTIVATION, MODERATE_DC, 5.0f)) skillRating /= 2 ;
    if (! lab.stocks.hasEnough(POWER)) skillRating /= 2 ;
    //
    //  Use the seed in the lab to create seed for the different crop types.
    if (Rand.num() < successChance * skillRating) {
      for (int var : Plantation.ALL_VARIETIES) {
        float quality = skillRating * Rand.avgNums(2) ;
        final Service yield = Plantation.speciesYield(var) ;
        quality += structure.upgradeBonus(yield) ;
        
        Item seed = Item.withType(GENE_SEED, new Crop(var)) ;
        seed = Item.withQuality(seed, (int) Visit.clamp(quality, 0, 5)) ;
        seed = Item.withAmount(seed, 1) ;
        if (var == Plantation.VAR_SAPLINGS) stocks.addItem(seed) ;
        else actor.gear.addItem(seed) ;
      }
      stocks.bumpItem(GENE_SEED, -0.2f) ;
      return true ;
    }
    return false ;
  }
  
  
  public boolean actionDeliverSeed(Actor actor, Plantation nursery) {
    actor.gear.transfer(GENE_SEED, nursery) ;
    return true ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    //
    //  Increment demand for gene seed-
    stocks.incDemand(GENE_SEED, 5, 1) ;
    final float decay = 0.1f / World.STANDARD_DAY_LENGTH ;
    for (Item seed : stocks.matches(GENE_SEED)) if (seed.refers != null) {
      stocks.removeItem(Item.withAmount(seed, decay)) ;
    }
    //
    //  Then update the current set of allotments-
    if (numUpdates % 10 == 0) {
      final int STRIP_SIZE = 4 ;
      int numCovered = 0 ;
      //
      //  First of all, remove any missing allotments (and their siblings in
      //  the same strip.)
      for (Plantation p : allotments) {
        if (p.destroyed()) {
          allotments.remove(p) ;
          for (Plantation s : p.strip) if (s != p) {
            s.structure.setState(Structure.STATE_SALVAGE, -1) ;
          }
        }
        else if (p.type == Plantation.TYPE_COVERED) numCovered++ ;
      }
      //
      //  Then, calculate how many allotments one should have.
      int maxAllots = 3 + (structure.upgradeBonus(Background.FIELD_HAND) * 2) ;
      maxAllots *= STRIP_SIZE ;
      if (maxAllots > allotments.size()) {
        //
        //  If you have too few, try to find a place for more-
        final boolean covered = numCovered <= allotments.size() / 3 ;
        Plantation allots[] = Plantation.placeAllotment(
          this, covered ? STRIP_SIZE : STRIP_SIZE, covered
        ) ;
        if (allots != null) for (Plantation p : allots) {
          allotments.add(p) ;
        }
      }
      if (maxAllots + STRIP_SIZE < allotments.size()) {
        //
        //  And if you have too many, flag the least productive for salvage.
        float minRating = Float.POSITIVE_INFINITY ;
        Plantation toRemove[] = null ;
        for (Plantation p : allotments) {
          final float rating = Plantation.rateArea(p.strip, world) ;
          if (rating < minRating) { toRemove = p.strip ; minRating = rating ; }
        }
        if (toRemove != null) for (Plantation p : toRemove) {
          p.structure.setState(Structure.STATE_SALVAGE, -1) ;
        }
      }
    }
  }
  

  public int numOpenings(Background v) {
    int num = super.numOpenings(v) ;
    if (v == Background.FIELD_HAND) return num + 1 ;
    if (v == Background.ECOLOGIST ) return num + 1 ;
    return 0 ;
  }
  
  
  protected List <Plantation> allotments() {
    return allotments ;
  }
  
  
  protected float growBonus(Tile t, int varID, boolean natural) {
    final float pollution = t.world.ecology().squalorRating(t) / 10f ;
    if (pollution > 0) return 0 ;
    final float hB = 1 - pollution ;
    float bonus = 1 ;
    if (varID == 4) {
      if (natural) return hB ;
      return (structure.upgradeBonus(PROTEIN) + 2) * 0.1f * bonus * hB ;
    }
    //
    //  Crops are, in sequence, rice, wheat, lily-things, tubers/veg and grubs.
    bonus = Math.max(0, (t.habitat().moisture() - 5) / 5f) ;
    if (varID == 1 || varID == 3) bonus = (1 - bonus) / 2f ;  //Dryland crops.
    if (varID < 2) {
      if (natural) return 1.0f * bonus * hB ;
      return (structure.upgradeBonus(CARBS) + 2) * 1.0f * bonus * hB ;
    }
    else {
      if (natural) return 0.5f * bonus * hB ;
      return (structure.upgradeBonus(GREENS) + 2) * 0.5f * bonus * hB ;
    }
  }
  
  
  public Service[] services() {
    return new Service[] { GREENS, PROTEIN, CARBS } ;
  }
  
  
  protected Background[] careers() {
    return new Background[] { Background.ECOLOGIST, Background.FIELD_HAND } ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  final static float GOOD_DISPLAY_OFFSETS[] = {
     -0.0f, 0,
     -1.0f, 0,
     -2.0f, 0,
     -0.0f, 1,
  } ;
  
  
  protected float[] goodDisplayOffsets() {
    return GOOD_DISPLAY_OFFSETS ;
  }
  
  
  protected Service[] goodsToShow() {
    return new Service[] { GENE_SEED, GREENS, PROTEIN, CARBS } ;
  }
  
  protected float goodDisplayAmount(Service good) {
    if (good == GENE_SEED) return stocks.amountOf(good) > 0 ? 5 : 0 ;
    return super.goodDisplayAmount(good) ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/nursery_button.gif") ;
  }
  
  
  public String fullName() { return "Botanical Station" ; }
  
  
  public String helpInfo() {
    return
      "Botanical Stations are responsible for agriculture and forestry, "+
      "helping to secure food supplies and advance terraforming efforts." ;
  }
  
  
  public String buildCategory() {
    return InstallTab.TYPE_ECOLOGIST ;
  }
}







