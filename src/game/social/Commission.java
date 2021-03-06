


package src.game.social ;
import src.game.actors.* ;
import src.game.common.* ;
import src.game.building.* ;
import src.user.* ;
import src.util.* ;



//
//  Purchase commodities for home.
//  Purchase a device/weapon, or outfit/armour.
//  Purchase rations, fuel cells or a medkit.



public class Commission extends Plan {
  
  
  
  /**  Data fields, construction and save/load methods-
    */
  final Item item ;
  final Venue shop ;
  
  private Manufacture order = null ;
  private float orderDate = -1 ;
  private boolean delivered = false ;
  
  
  public Commission(Actor actor, Item baseItem, Venue shop) {
    super(actor, baseItem.type, shop) ;
    this.item = Item.withReference(baseItem, actor) ;
    this.shop = shop ;
  }
  
  
  public Commission(Session s) throws Exception {
    super(s) ;
    item = Item.loadFrom(s) ;
    shop = (Venue) s.loadObject() ;
    order = (Manufacture) s.loadObject() ;
    orderDate = s.loadFloat() ;
    delivered = s.loadBool() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    Item.saveTo(s, item) ;
    s.saveObject(shop) ;
    s.saveObject(order) ;
    s.saveFloat(orderDate) ;
    s.saveBool(delivered) ;
  }
  
  
  
  /**  Assessing and locating targets-
    */
  public float priorityFor(Actor actor) {
    if (order != null && ! order.finished()) return 0 ;
    final float business = shop.stocks.specialOrders().size() ;
    final int price = item.price() ;
    if (price > actor.gear.credits()) return 0 ;
    final float costVal = actor.mind.greedFor(price) * CASUAL ;
    float priority = 1 + ROUTINE - (costVal + business) ;
    return Visit.clamp(priority, 0, ROUTINE) ;
  }
  
  
  public static Venue findVenue(Actor actor, Item item) {
    final Presences p = actor.world().presences ;
    return (Venue) p.nearestMatch(item.type, actor, -1) ;
  }
  
  
  private boolean expired() {
    if (orderDate == -1) return false ;
    final int maxTime = World.STANDARD_DAY_LENGTH * 2 ;
    if (actor.world().currentTime() - orderDate > maxTime) return true ;
    final boolean
      ongoing = shop.stocks.specialOrders().contains(order),
      hasItem = shop.stocks.hasItem(item) ;
    if (ongoing || hasItem) return false ;
    return true ;
  }
  
  
  public boolean finished() {
    return delivered || expired() ;
  }
  
  
  
  /**  Behaviour implementation-
    */
  protected Behaviour getNextStep() {
    if (finished()) return null ;
    //
    //  TODO:  Check that someone is attending the shop.
    if (order != null && order.finished()) {
      final Action pickup = new Action(
        actor, shop,
        this, "actionPickupItem",
        Action.REACH_DOWN, "Collecting"
      ) ;
      return pickup ;
    }
    
    if (order == null) {
      final Action placeOrder = new Action(
        actor, shop,
        this, "actionPlaceOrder",
        Action.TALK_LONG, "Placing Order"
      ) ;
      return placeOrder ;
    }
    return null ;
  }
  
  
  public boolean actionPlaceOrder(Actor actor, Venue shop) {
    order = new Manufacture(null, shop, item.type.materials(), item) ;
    shop.stocks.addSpecialOrder(order) ;
    orderDate = shop.world().currentTime() ;
    return true ;
  }
  
  
  public boolean actionPickupItem(Actor actor, Venue shop) {
    final int price = item.price() ;
    shop.inventory().incCredits(price) ;
    actor.inventory().incCredits(0 - price) ;
    
    shop.inventory().removeItem(item) ;
    actor.inventory().addItem(item) ;
    delivered = true ;
    I.say(actor+" picking up: "+item) ;
    return true ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public void describeBehaviour(Description d) {
    if (order == null) {
      d.append("Placing order for "+item+" at ") ;
      d.append(shop) ;
    }
    else {
      d.append("Collecting "+item+" at ") ;
      d.append(shop) ;
    }
  }
}





