/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.common.* ;
import src.util.* ;
import src.user.* ;



/**  An inventory allows for the storage, transfer and tracking of discrete
  *  items.
  */
public class Inventory {
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final public Owner owner ;
  protected float credits, taxed ;
  
  private Table <Item, Item> itemTable = new Table(10) ;
  //  private int sumGoods ;
  
  
  public Inventory(Owner owner) {
    this.owner = owner ;
  }
  
  
  public static interface Owner extends Target, Session.Saveable {
    Inventory inventory() ;
    float priceFor(Service service) ;
    int spaceFor(Service good) ;
    void afterTransaction(Item item, float amount) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(itemTable.size()) ;
    for (Item item : itemTable.values()) Item.saveTo(s, item) ;
    s.saveFloat(credits) ;
    s.saveFloat(taxed  ) ;
  }
  
  
  public void loadState(Session s) throws Exception {
    for (int i = s.loadInt() ; i-- > 0 ;) {
      final Item item = Item.loadFrom(s) ;
      itemTable.put(item, item) ;
    }
    credits = s.loadFloat() ;
    taxed   = s.loadFloat() ;
  }
  
  
  /**  Writes inventory information to the given text field.
    */
  public void writeInformation(Description text) {
    for (Item item : itemTable.values()) {
      text.append("\n") ;
      text.insert(item.type.pic, 25) ;
      text.append(item) ;
      //text.append("\n"+item) ;
    }
    if (credits() != 0) {
      text.append("\n"+((int) credits())+" credits") ;
      text.append((credits() < 0) ? " (in debt)" : "") ;
    }
    text.append("\n") ;
  }
  
  
  
  /**  Financial balance-
    */
  public void incCredits(float inc) {
    if (inc > 0) {
      credits += inc ;
    }
    else {
      credits += inc ;
      if (credits < 0) {
        taxed += credits ;
        credits = 0 ;
      }
    }
    //owner.afterTransaction() ;
  }
  
  
  public float credits() {
    return credits + taxed ;
  }
  
  
  public float unTaxed() {
    return credits ;
  }
  
  
  public void taxDone() {
    taxed += credits ;
    credits = 0 ;
    //owner.afterTransaction() ;
  }
  
  
  
  /**  Returns whether this inventory is empty.
   */
  public boolean empty() {
    return itemTable.size() == 0 && (credits + taxed) <= 0 ;
  }
  
  
  public Batch <Item> allItems() {
    final Batch <Item> allItems = new Batch <Item> () ;
    for (Item item : itemTable.values()) allItems.add(item) ;
    return allItems ;
  }
  
  
  public void clearItems(Service type) {
    itemTable.remove(type) ;
  }
  
  
  public void removeAllItems() {
    itemTable.clear() ;
    credits = taxed = 0 ;
    //owner.afterTransaction() ;
  }
  
  
  
  /**  Adds the given item to the inventory.  If the item is one with 'free
    *  terms' used for matching purposes, returns false- only fully-specified
    *  items can be added.
    */
  public boolean addItem(Item item) {
    if (item.isMatch() || item.amount <= 0) {
      I.sayAbout(owner, "Adding null item... "+item.amount) ;
      new Exception().printStackTrace() ;
      return false ;
    }
    //
    //  Check to see if a similar item already exists.
    final Item oldItem = itemTable.get(item) ;
    float amount = item.amount ;
    if (oldItem != null) amount += oldItem.amount ;
    final Item entered = Item.withAmount(item, amount) ;
    itemTable.put(entered, entered) ;
    if (owner != null) owner.afterTransaction(item, item.amount) ;
    return true ;
  }
  
  
  public void bumpItem(Service type, float amount) {
    if (amount == 0) return ;
    if (amount > 0) addItem(Item.withAmount(type, amount)) ;
    else removeItem(Item.withAmount(type, 0 - amount)) ;
  }
  
  
  public void bumpItem(Service type, float amount, int max) {
    final float oldAmount = amountOf(type) ;
    bumpItem(type, Visit.clamp(amount, 0 - oldAmount, max - oldAmount)) ;
  }
  
  
  
  /**  Removes the given item from this inventory.  The item given must have a
    *  single unique match, and the match must be greater in amount.  Returns
    *  false otherwise.
    */
  public boolean removeItem(Item item) {
    if (item.isMatch() || item.amount <= 0) {
      I.sayAbout(owner, "Adding null item... "+item.amount) ;
      new Exception().printStackTrace() ;
      return false ;
    }
    //
    //  Check to see if the item already exists-
    final Item oldItem = itemTable.get(item) ;
    if (oldItem == null) return false ;
    if (oldItem.amount <= item.amount) {
      itemTable.remove(item) ;
      if (owner != null) owner.afterTransaction(item, oldItem.amount) ;
      return false ;
    }
    final float newAmount = oldItem.amount - item.amount ;
    final Item entered = Item.withAmount(item, newAmount) ;
    itemTable.put(entered, entered) ;
    if (owner != null) owner.afterTransaction(item, item.amount - newAmount) ;
    return true ;
  }
  

  public float transfer(Service type, Owner to) {
    float amount = 0 ;
    for (Item item : matches(type)) {
      removeItem(item) ;
      to.inventory().addItem(item) ;
      amount += item.amount ;
    }
    return amount ;
  }
  
  
  public float transfer(Item item, Owner to) {
    final float amount = Math.min(item.amount, amountOf(item)) ;
    final Item transfers = Item.withAmount(item, amount) ;
    removeItem(transfers) ;
    to.inventory().addItem(transfers) ;
    return amount ;
  }
  
  
  
  /**  Finding matches and quantities-
    */
  public Batch <Item> matches(Item item) {
    final Batch <Item> matches = new Batch <Item> (4) ;
    for (Item found : itemTable.values()) {
      if (item.matches(found)) matches.add(found) ;
    }
    return matches ;
  }
  
  
  public Batch <Item> matches(Service type) {
    final Batch <Item> matches = new Batch <Item> (4) ;
    for (Item found : itemTable.values()) {
      if (found.type == type) matches.add(found) ;
    }
    return matches ;
  }
  
  
  public float amountOf(Item item) {
    if (item.isMatch()) {
      float amount = 0 ;
      for (Item found : itemTable.values()) {
        if (item.matchKind(found)) amount += found.amount ;
      }
      return amount ;
    }
    else {
      final Item found = itemTable.get(item) ;
      return found == null ? 0 : found.amount ;
    }
  }
  
  
  public float amountOf(Service type) {
    return amountOf(Item.withType(type)) ;
  }
  
  
  
  /**  Returns whether this inventory has enough of the given item to satisfy
    *  match criteria.
    */
  public boolean hasItem(Item item) {
    final float amount = amountOf(item) ;
    if (item.amount == Item.ANY) return amount > 0 ;
    else return item.amount > 0 && amount >= item.amount ;
  }
}




