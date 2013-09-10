


package src.game.base ;
import src.game.building.Paving ;
import src.game.common.* ;
import src.graphics.common.Model ;
import src.graphics.common.Texture ;
import src.graphics.cutout.ImageModel ;
import src.util.* ;




public class MagLineNode extends Element implements
  TileConstants, Schedule.Updates
{
  
  
  /**  Constants, fields, constructors and save/load functions.
    */
  final Base base ;
  private int facing ;
  private boolean isHub = false ;
  private Tile around[] = new Tile[9] ;
  //private Paving paving ;
  
  
  MagLineNode(Base base) {
    super() ;
    this.base = base ;
  }
  
  
  public MagLineNode(Session s) throws Exception {
    super(s) ;
    this.facing = s.loadInt() ;
    this.base = (Base) s.loadObject() ;
    this.isHub = s.loadBool() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(facing) ;
    s.saveObject(base) ;
    s.saveBool(isHub) ;
  }
  
  
  
  /**  Life cycle and placement-
    */
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
    world.terrain().maskAsPaved(origin().vicinity(around), true) ;
    if (isHub) {
      base.paving.updateJunction(origin(), true) ;
      //base.paving.toggleJunction(this, origin(), true) ;
      //paving.onWorldEntry() ;
      //world.schedule.scheduleForUpdates(this) ;
    }
  }


  public void exitWorld() {
    if (isHub) {
      base.paving.updateJunction(origin(), false) ;
      //base.paving.toggleJunction(this, origin(), false) ;
      //paving.onWorldExit() ;
      //world.schedule.unschedule(this) ;
    }
    world.terrain().maskAsPaved(origin().vicinity(around), false) ;
    super.exitWorld() ;
  }
  
  
  public float scheduledInterval() {
    return 10 ;
  }
  
  
  public void updateAsScheduled(int numUpdates) {
  }
  
  /*
  public Tile mainEntrance() {
    return origin() ;
  }
  
  
  public Paving paving() {
    return paving ;
  }
  
  
  public boolean usesRoads() {
    return true ;
  }
  
  public Tile[] surrounds() {
    return origin().vicinity(around) ;
  }


  public Base base() {
    return base ;
  }
  
  
  public Box2D area() {
    return area(null) ;
  }
  //*/
  
  
  public int pathType() {
    return Tile.PATH_HINDERS ;
  }
  
  
  public int owningType() {
    return Element.VENUE_OWNS ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  final static ImageModel
    NODE_MODELS[] = ImageModel.loadModels(
      MagLineNode.class, 1, 0.1f, "media/Buildings/merchant/",
      "mag_node_left.png",
      "mag_node_right.png",
      "mag_node_centre.png",
      "mag_node_flat.png"
    ),
    NODE_MODEL_LEFT   = NODE_MODELS[0],
    NODE_MODEL_RIGHT  = NODE_MODELS[1],
    NODE_MODEL_CENTRE = NODE_MODELS[2],
    NODE_MODEL_FLAT   = NODE_MODELS[3] ;
  
  
  public String toString() {
    return "Mag Node" ;
  }
  
  
  //
  //  NOTE:  This method should only be called when the node is first laid
  //  down.  TODO:  Move to within constructor?
  //  TODO:  Separate the model-selection and functional aspects of this
  //  process better.
  void updateFacing() {
    final Model model = updateModel() ;
    attachSprite(model.makeSprite()) ;
  }
  
  
  private ImageModel updateModel() {
    final Tile o = origin() ;
    o.allAdjacent(around) ;
    int numNear = 0 ;
    for (int n : N_ADJACENT) if (isNode(n)) numNear++ ;
    if (numNear != 2) {
      isHub = true ;
      return NODE_MODEL_CENTRE ;
    }
    if (isNode(N) && isNode(S)) {
      if (o.y % 6 == 0) isHub = true ;
      if (o.y % 3 == 0) return NODE_MODEL_FLAT ;
      return NODE_MODEL_RIGHT ;
    }
    if (isNode(W) && isNode(E)) {
      if (o.x % 6 == 0) isHub = true ;
      if (o.x % 3 == 0) return NODE_MODEL_FLAT ;
      return NODE_MODEL_LEFT  ;
    }
    return NODE_MODEL_CENTRE ;
  }
  
  
  private boolean isNode(int dir) {
    final Tile t = around[dir] ;
    if (t == null) return false ;
    return
      t.flaggedWith() instanceof Causeway ||
      t.owner() instanceof MagLineNode ;
  }
}  









