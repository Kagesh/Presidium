/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.social.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;



public class StockExchange extends Venue implements BuildConstants {
  
  
  /**  Data fields, constructors and save/load functionality-
    */
  final static Model
    MODEL = ImageModel.asIsometricModel(
      StockExchange.class,
      "media/Buildings/merchant/stock_exchange.png",
      4, 2
    ) ;
  
  
  private CargoBarge cargoBarge ;
  
  
  
  public StockExchange(Base base) {
    super(4, 2, ENTRANCE_SOUTH, base) ;
    personnel.setShiftType(SHIFTS_BY_DAY) ;
    structure.setupStats(
      150, 3, 250,
      Structure.NORMAL_MAX_UPGRADES, Structure.TYPE_VENUE
    ) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public StockExchange(Session s) throws Exception {
    super(s) ;
    personnel.setShiftType(SHIFTS_ALWAYS) ;
    cargoBarge = (CargoBarge) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(cargoBarge) ;
  }
  
  
  public void onCompletion() {
    super.onCompletion() ;
    cargoBarge = new CargoBarge() ;
    cargoBarge.assignBase(base()) ;
    cargoBarge.setHangar(this) ;
    final Tile o = origin() ;
    cargoBarge.enterWorldAt(o.x, o.y, world) ;
    cargoBarge.goAboard(this, world) ;
  }
  
  
  public CargoBarge cargoBarge() {
    return cargoBarge ;
  }
  
  
  
  /**  Upgrades, behaviour and economic functions-
    */
  final static Index <Upgrade> ALL_UPGRADES = new Index <Upgrade> (
    StockExchange.class, "stock_exchange_upgrades"
  ) ;
  public Index <Upgrade> allUpgrades() { return ALL_UPGRADES ; }
  final public static Upgrade
    
    //  These two categories get space at the front of the building...
    RATIONS_STOCK = new Upgrade(
      "Rations Stock",
      "Increases space available to carbs, greens, protein and soma, and "+
      "augments profits from their sale.",
      150, null, 1, null, ALL_UPGRADES
    ),
    
    HARDWARE_STOCK = new Upgrade(
      "Hardware Stock",
      "Increases space available to parts, plastics, circuitry and decor, and "+
      "augments profits from their sale.",
      150, null, 1, null, ALL_UPGRADES
    ),
    
    //  ...and these two get space in the back.
    MEDICAL_DISPENSARY = new Upgrade(
      "Medical Dispenary",
      "Increases space available to stim kits, soma, medicine and gene seed, "+
      "and augments profits from their sale.",
      250, null, 1, RATIONS_STOCK, ALL_UPGRADES
    ),
    
    PROSPECT_EXCHANGE = new Upgrade(
      "Prospect Exchange",
      "Increases space available to metal ore, petrocarbs, fuel cores and "+
      "rarities, and augments profits from their sale.",
      250, null, 1, HARDWARE_STOCK, ALL_UPGRADES
    ),
    
    //  While these two don't show up visually at all.
    VENDOR_STATION = new Upgrade(
      "Vendor Station",
      "Vendors are responsible for transport and presentation of both "+
      "essential commodities and luxury goods.",
      100, Background.STOCK_VENDOR, 1, null, ALL_UPGRADES
    ),
    
    ARMS_DEALING = new Upgrade(
      "Arms Dealing",
      "Increases space available to power cells, arms and armour, "+
      "and augments profit from their sale.",
      200, null, 1, VENDOR_STATION, ALL_UPGRADES
    ) ;
  
  
  public int numOpenings(Background p) {
    final int nO = super.numOpenings(p) ;
    if ( p == Background.STOCK_VENDOR) return nO + 2 ;
    return 0 ;
  }
  
  
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null ;
    final Choice choice = new Choice(actor) ;
    cargoBarge.setHangar(this) ;  //Might not be needed anymore...
    //
    //  See if there's a bulk delivery to be made-
    final Batch <Venue> depots = Deliveries.nearbyDepots(this, world) ;
    final Delivery bD = Deliveries.nextDeliveryFor(
      actor, this, ALL_COMMODITIES, depots, 50, world
    ) ;
    if (bD != null && personnel.assignedTo(bD) < 1) {
      bD.priorityMod = Plan.CASUAL ;
      bD.driven = cargoBarge ;
      choice.add(bD) ;
    }
    final Delivery bC = Deliveries.nextCollectionFor(
      actor, this, ALL_COMMODITIES, depots, 50, null, world
    ) ;
    if (bC != null && personnel.assignedTo(bC) < 1) {
      bC.priorityMod = Plan.CASUAL ;
      bC.driven = cargoBarge ;
      choice.add(bC) ;
    }
    //
    //  Otherwise, consider local deliveries and supervision of the venue-
    final Delivery d = Deliveries.nextDeliveryFor(
      actor, this, services(), 10, world
    ) ;
    if (d != null && personnel.assignedTo(d) < 1) choice.add(d) ;
    final Delivery c = Deliveries.nextCollectionFor(
      actor, this, services(), 10, null, world
    ) ;
    if (c != null && personnel.assignedTo(c) < 1) choice.add(c) ;
    choice.add(new Supervision(actor, this)) ;
    return choice.weightedPick(actor.mind.whimsy()) ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
    if (! structure.intact()) return ;
    
    final Batch <Venue> depots = Deliveries.nearbyDepots(this, world) ;
    for (Service type : ALL_COMMODITIES) {
      final int demandBonus = (upgradeForGood(type) * 10) - 5 ;
      if (demandBonus < 0) continue ;
      stocks.incDemand(type, demandBonus, 1) ;
      stocks.diffuseDemand(type, depots) ;
    }
  }
  
  
  public void afterTransaction(Item item, float amount) {
    super.afterTransaction(item, amount) ;
    /*
    if (amount > 0) {
      final float saleBonus = (upgradeForGood(item.type) + 1) / 20f ;
      if (saleBonus <= 0) return ;
      stocks.incCredits(amount * priceFor(item.type) * saleBonus) ;
    }
    //*/
  }
  
  
  public int spaceFor(Service good) {
    switch (upgradeForGood(good)) {
      case (-1) : return 0  ;
      case ( 0) : return 20 ;
      case ( 1) : return 35 ;
      case ( 2) : return 45 ;
      case ( 3) : return 50 ;
    }
    return 0 ;
  }
  
  
  private int upgradeForGood(Service type) {
    final Integer key = (Integer) SupplyDepot.SERVICE_KEY.get(type) ;
    final Upgrade KU ;
    if (type instanceof DeviceType) KU = ARMS_DEALING ;
    else if (key == null) return -1 ;
    else if (key == SupplyDepot.KEY_RATIONS ) KU = RATIONS_STOCK      ;
    else if (key == SupplyDepot.KEY_MINERALS) KU = PROSPECT_EXCHANGE  ;
    else if (key == SupplyDepot.KEY_MEDICAL ) KU = MEDICAL_DISPENSARY ;
    else if (key == SupplyDepot.KEY_BUILDING) KU = HARDWARE_STOCK     ;
    else return -1 ;
    return structure.upgradeLevel(KU) ;
  }
  
  
  protected Background[] careers() {
    return new Background[] { Background.STOCK_VENDOR } ;
  }
  
  
  public Service[] services() {
    return ALL_COMMODITIES ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  final static float GOOD_DISPLAY_OFFSETS[] = {
    -0.5f, 0,
    -1.0f, 0,
    -1.5f, 0,
    -2.0f, 0,
     0, 0.5f,
     0, 1.0f,
     0, 1.5f,
     0, 2.0f,
  } ;
  
  
  protected float[] goodDisplayOffsets() {
    return GOOD_DISPLAY_OFFSETS ;
  }
  
  
  protected Service[] goodsToShow() {
    return new Service[] {
      CARBS, PROTEIN, GREENS, SOMA,
      PARTS, PLASTICS, CIRCUITRY, DECOR
    } ;
  }
  
  //
  //  TODO:  You have to show items in the back as well, behind a sprite
  //  overlay for the facade of the structure.
  protected float goodDisplayAmount(Service good) {
    return super.goodDisplayAmount(good) / 2f ;
  }
  
  
  public String fullName() {
    return "Stock Exchange" ;
  }


  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/stock_exchange_button.gif") ;
  }


  public String helpInfo() {
    return
      "The Stock Exchange facilitates small-scale purchases within the "+
      "neighbourhood, and bulk transactions between local merchants." ;
  }


  public String buildCategory() {
    return UIConstants.TYPE_MERCHANT ;
  }
}





