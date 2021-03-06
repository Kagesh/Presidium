


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.jointed.* ;
import src.graphics.widgets.HUD ;
import src.user.* ;
import src.util.* ;



public class CargoBarge extends Vehicle implements
  Inventory.Owner, BuildConstants
{
  
  /**  Fields, constants, constructors and save/load methods-
    */
  final static String
    FILE_DIR = "media/Vehicles/",
    XML_PATH = FILE_DIR+"VehicleModels.xml" ;
  final static Model
    BARGE_MODEL = MS3DModel.loadMS3D(
      CargoBarge.class, FILE_DIR, "loader_2.ms3d", 1.0f
    ).loadXMLInfo(XML_PATH, "CargoBarge") ;
  
  
  
  public CargoBarge() {
    super() ;
    attachSprite(BARGE_MODEL.makeSprite()) ;
  }
  
  
  public CargoBarge(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  public float height() { return 1.0f ; }
  public float radius() { return 1.0f ; }
  
  
  
  /**  Economic and behavioural functions-
    */
  public Behaviour jobFor(Actor actor) {
    return null ;
  }
  
  
  public boolean actionBoard(Actor actor, CargoBarge ship) {
    ship.setInside(actor, true) ;
    return true ;
  }
  
  /*
  protected void offloadPassengers() {
    final int size = 2 * (int) Math.ceil(radius()) ;
    final int EC[] = Spacing.entranceCoords(size, size, entranceFace) ;
    final Box2D site = this.area(null) ;
    final Tile o = world.tileAt(site.xpos() + 0.5f, site.ypos() + 0.5f) ;
    final Tile exit = world.tileAt(o.x + EC[0], o.y + EC[1]) ;
    this.dropPoint = exit ;
    
    for (Mobile m : inside()) if (! m.inWorld()) {
      m.enterWorldAt(exit.x, exit.y, world) ;
    }
    inside.clear() ;
  }
  //*/
  
  
  public void updateAsScheduled(int numUpdates) {
    super.updateAsScheduled(numUpdates) ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Cargo Barge" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return null ;
  }
  
  
  public String helpInfo() {
    return
      "Cargo Barges shuttle goods in bulk between the more distant reaches "+
      "of your settlement.\n\n"+
      "  'She kicks like a mule and stinks of raw carbons, but she'll "+
      "getcha from A to B.  Assuming B is downhill.'\n"+
      "  -Tev Marlo, Supply Corps" ;
  }
}



