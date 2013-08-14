/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.actors.* ;
import src.game.base.* ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.user.* ;
import src.util.* ;



public abstract class Vehicle extends Mobile implements
  Boardable, Inventory.Owner, CitizenAI.Employment, Selectable
{
  
  
  /**  Fields, constants, constructors and save/load methods-
    */
  final public Inventory cargo = new Inventory(this) ;
  final protected List <Mobile> inside = new List <Mobile> () ;
  final protected List <Actor> crew = new List <Actor> () ;
  
  protected Venue dropPoint ;
  protected float entranceFace = Venue.ENTRANCE_NONE ;
  
  
  public Vehicle() {
    super() ;
  }

  public Vehicle(Session s) throws Exception {
    super(s) ;
    cargo.loadState(s) ;
    s.loadObjects(inside) ;
    s.loadObjects(crew) ;
    dropPoint = (Venue) s.loadObject() ;
    entranceFace = s.loadFloat() ;
  }
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    cargo.saveState(s) ;
    s.saveObjects(inside) ;
    s.saveObjects(crew) ;
    s.saveObject(dropPoint) ;
    s.saveFloat(entranceFace) ;
  }
  
  
  public Base base() {
    if (dropPoint != null) return dropPoint.base() ;
    return null ;
  }
  
  
  
  /**  Assigning jobs to crew members-
    */
  public Behaviour jobFor(Actor actor) {
    return null ;
  }
  
  
  public void setWorker(Actor actor, boolean is) {
    if (is) crew.include(actor) ;
    else crew.remove(actor) ;
  }
  
  
  public List <Actor> crew() {
    return crew ;
  }
  
  
  
  /**  Handling passengers and cargo-
    */
  public void setInside(Mobile m, boolean is) {
    if (is) {
      inside.include(m) ;
    }
    else {
      inside.remove(m) ;
    }
  }
  
  
  public List <Mobile> inside() {
    return inside ;
  }
  

  public Boardable[] canBoard(Boardable batch[]) {
    if (batch == null) batch = new Boardable[1] ;
    else for (int i = batch.length ; i-- > 0 ;) batch[i] = null ;
    batch[0] = dropPoint ;
    return batch ;
  }
  
  
  public boolean isEntrance(Boardable b) {
    return dropPoint == b ;
  }
  
  
  public boolean allowsEntry(Mobile m) {
    return m.base() == base() ;
  }
  
  
  public Box2D area(Box2D put) {
    if (put == null) put = new Box2D() ;
    final Vec3D p = position ;
    final float r = radius() ;
    put.set(p.x - (r / 2), p.y - (r / 2), r, r) ;
    return put ;
  }
  

  public Inventory inventory() { return cargo ; }
  
  
  //  Intended for override by subclasses.
  public boolean landed() {
    return true ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String[] infoCategories() {
    return null ;  //cargo, passengers, integrity.
  }
  
  
  public InfoPanel createPanel(BaseUI UI) {
    return new InfoPanel(UI, this, InfoPanel.DEFAULT_TOP_MARGIN) ;
  }

  
  
  public void renderSelection(Rendering rendering, boolean hovered) {
    if (indoors()) return ;
    Selection.renderPlane(
      rendering, viewPosition(null), radius() + 0.5f,
      hovered ? Colour.transparency(0.5f) : Colour.WHITE,
      Selection.SELECT_CIRCLE
    ) ;
  }
  
  
  public Target subject() {
    return this ;
  }
  

  public String toString() {
    return fullName() ;
  }
  
  
  public void whenClicked() {
    if (PlayLoop.currentUI() instanceof BaseUI) {
      ((BaseUI) PlayLoop.currentUI()).selection.setSelected(this) ;
    }
  }
}











