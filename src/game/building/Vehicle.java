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
import src.graphics.widgets.HUD;
import src.user.* ;
import src.util.* ;




//
//  TODO:  This probably needs to implement the Structural interface.


public abstract class Vehicle extends Mobile implements
  Boardable, Inventory.Owner, HumanAI.Employment,
  Selectable, BuildConstants, Installation
{
  
  
  /**  Fields, constants, constructors and save/load methods-
    */
  protected Base base ;
  final public Inventory cargo = new Inventory(this) ;
  final public Structure structure = new Structure(this) ;
  
  final protected List <Mobile> inside = new List <Mobile> () ;
  final protected List <Actor> crew = new List <Actor> () ;
  
  private Actor pilot ;
  private Venue hangar ;
  private float pilotBonus ;
  
  protected float entranceFace = Venue.ENTRANCE_NONE ;
  protected Boardable dropPoint ;
  
  
  public Vehicle() {
    super() ;
    structure.setState(Structure.STATE_INTACT, 1) ;
  }

  public Vehicle(Session s) throws Exception {
    super(s) ;
    cargo.loadState(s) ;
    structure.loadState(s) ;
    s.loadObjects(inside) ;
    s.loadObjects(crew) ;
    dropPoint = (Boardable) s.loadTarget() ;
    entranceFace = s.loadFloat() ;
    base = (Base) s.loadObject() ;
    pilot = (Actor) s.loadObject() ;
    hangar = (Venue) s.loadObject() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    cargo.saveState(s) ;
    structure.saveState(s) ;
    s.saveObjects(inside) ;
    s.saveObjects(crew) ;
    s.saveTarget(dropPoint) ;
    s.saveFloat(entranceFace) ;
    s.saveObject(base) ;
    s.saveObject(pilot) ;
    s.saveObject(hangar) ;
  }
  
  
  public void assignBase(Base base) {
    this.base = base ;
  }
  
  
  public Base base() {
    return base ;
  }
  
  
  
  /**  Pilot and hangar configuration-
    */
  public boolean canPilot(Actor actor) {
    if (pilot != null && actor != null && actor != pilot) {
      return false ;
    }
    return true ;
  }
  
  
  public boolean setPilot(Actor actor) {
    if (! canPilot(actor)) {
      //I.complain("CANNOT SET AS PILOT") ;
      return false ;
    }
    this.pilot = actor ;
    return true ;
  }
  
  
  public Actor pilot() {
    return pilot ;
  }
  
  
  public void setHangar(Venue hangar) {
    this.hangar = hangar ;
    assignBase(hangar.base()) ;
  }
  
  
  public Venue hangar() {
    return hangar ;
  }
  
  
  
  /**  Dealing with items, inventory and structural requirements-
    */
  public Inventory inventory() {
    return cargo ;
  }
  
  
  public float priceFor(Service service) {
    return service.basePrice ;
  }
  
  
  public int spaceFor(Service good) {
    return structure.maxIntegrity() ;//- cargo.spaceUsed() ;
  }
  
  
  public void afterTransaction(Item item, float amount) {
  }
  
  

  public Index<Upgrade> allUpgrades() {
    return null;
  }

  public void onCompletion() {
  }

  public void onDestruction() {
  }

  public Structure structure() {
    return structure ;
  }
  
  
  
  /**  Vehicles are generally commissioned as an accompaniment to venues by the
    *  venues themselves, so these methods aren't much used.
    */
  public int buildCost() { return structure.buildCost() ; }
  public String buildCategory() { return UIConstants.TYPE_HIDDEN ; }

  public boolean pointsOkay(Tile from, Tile to) { return false ; }
  public void doPlace(Tile from, Tile to) {}
  public void preview(
    boolean canPlace, Rendering rendering, Tile from, Tile to
  ) {}
  
  

  /**  Handling pathing-
    */
  protected MobilePathing initPathing() {
    return new MobilePathing(this) ;
  }
  
  
  protected void updateAsMobile() {
    super.updateAsMobile() ;
    if (pilot != null) updatePiloting() ;
    else pathing.updateTarget(pathing.target()) ;
    final Boardable step = pathing.nextStep() ;
    
    if (pathing.checkPathingOkay() && step != null) {
      float moveRate = baseMoveRate() ;
      if (origin().pathType() == Tile.PATH_ROAD) moveRate *= 1.5f ;
      if (origin().owner() instanceof Causeway) moveRate *= 1.5f ;
      moveRate *= (pilotBonus + 1) / 2 ;
      pathing.headTowards(step, moveRate, true) ;
    }
    else world.schedule.scheduleNow(this) ;
  }
  
  
  protected float baseMoveRate() {
    return 1.0f ;
  }
  
  
  protected void updatePiloting() {
    if (pilot.aboard() != this) {
      pathing.updateTarget(null) ;
      return ;
    }
    if (pilot.currentAction() == null) return ;
    pathing.updateTarget(pilot.currentAction().target()) ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
    structure.updateStructure(numUpdates) ;
    //
    //  TODO:  Restore this once building/salvage of vehicles is complete.
    ///if (! structure.intact()) return ;
    
    if (pilot != null && pilot.aboard() == this) {
      pilotBonus = 1 ;
      if (! pilot.traits.test(PILOTING, SIMPLE_DC, 0.5f)) pilotBonus /= 1.5f ;
      if (pilot.traits.test(PILOTING, MODERATE_DC, 0.5f)) pilotBonus *= 1.5f ;
    }
    else {
      pilotBonus = 0.5f ;
      pilot = null ;
    }
    if (! pathing.checkPathingOkay()) pathing.refreshPath() ;
    if (hangar != null && hangar.destroyed()) {
      //  TODO:  REGISTER FOR SALVAGE
      setAsDestroyed() ;
    }
  }
  
  /*
  public boolean blocksMotion(Boardable b) {
    if (super.blocksMotion(b)) return true ;
    if (b instanceof Tile && b != aboard()) {
      final Tile t = (Tile) b ;
      if (Spacing.distance(t, origin()) > MobilePathing.MAX_PATH_SCAN) {
        return false ; 
      }
      if (t.inside().size() > 0) return true ;
    }
    return false ;
  }
  //*/
  
  
  
  /**  TODO:  Include code here for assessing suitable landing sites?
    */

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
  
  
  public boolean actionDrive(Actor actor, Vehicle driven) {
    return true ;
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
    if (batch == null) batch = new Boardable[2] ;
    else for (int i = batch.length ; i-- > 0 ;) batch[i] = null ;
    batch[0] = dropPoint ;
    if (aboard() != null) batch[1] = aboard ;
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
    put.set(p.x - r, p.y - r, r * 2, r * 2) ;
    return put ;
  }
  
  
  public boolean landed() {
    return true ;
  }
  
  
  public Boardable dropPoint() {
    return dropPoint ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String[] infoCategories() {
    return null ;  //cargo, passengers, integrity.
  }
  
  
  public InfoPanel createPanel(BaseUI UI) {
    return new InfoPanel(UI, this, InfoPanel.DEFAULT_TOP_MARGIN) ;
  }
  
  
  protected float fogFor(Base base) {
    if (base == this.base) return 1 ;
    return super.fogFor(base) ;
  }
  

  public void renderSelection(Rendering rendering, boolean hovered) {
    if (indoors() || ! inWorld()) return ;
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
      ((BaseUI) PlayLoop.currentUI()).selection.pushSelection(this, false) ;
    }
  }
  
  
  public void describeStatus(Description d) {
    if (pilot != null && pilot.mind.rootBehaviour() != null) {
      pilot.mind.rootBehaviour().describeBehaviour(d) ;
    }
    else if (pathing.target() != null) {
      if (pathing.target() == aboard()) d.append("Aboard ") ;
      else d.append("Heading for ") ;
      d.append(pathing.target()) ;
    }
    else {
      d.append("Idling") ;
    }
  }
  
  
  public void writeInformation(Description d, int categoryID, HUD UI) {
    describeStatus(d) ;
    if (crew.size() > 0) d.appendList("\n\nCrew: ", crew) ;
    if (inside.size() > 0) d.appendList("\n\nPassengers: ", inside) ;
    if (! cargo.empty()) d.appendList("\n\nCargo: ", cargo.allItems()) ;
    d.append("\n\n") ; d.append(helpInfo(), Colour.LIGHT_GREY) ;
  }
}



