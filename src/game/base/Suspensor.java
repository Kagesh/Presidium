


package src.game.base ;
import src.game.common.* ;
import src.game.actors.* ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.graphics.jointed.* ;
import src.user.* ;
import src.util.* ;



//
//  TODO:  Only disappear once inside a venue.  Otherwise, stay where you are,
//  and wait for someone to pick you up.

public class Suspensor extends Mobile {
  
  

  final static String
    FILE_DIR = "media/Vehicles/",
    XML_PATH = FILE_DIR+"VehicleModels.xml" ;
  final static Model SUSPENSOR_MODEL = MS3DModel.loadMS3D(
    Suspensor.class, FILE_DIR, "Barge.ms3d", 0.015f
  ).loadXMLInfo(XML_PATH, "Suspensor") ;
  
  
  final Actor followed ;
  final Behaviour tracked ;
  
  public Actor passenger = null ;
  public Item cargo = null ;
  
  
  
  public Suspensor(Actor followed, Behaviour tracked) {
    super() ;
    this.followed = followed ;
    this.tracked = tracked ;
    attachSprite(SUSPENSOR_MODEL.makeSprite()) ;
  }
  
  
  public Suspensor(Session s) throws Exception {
    super(s) ;
    followed = (Actor) s.loadObject() ;
    tracked = (Behaviour) s.loadObject() ;
    passenger = (Actor) s.loadObject() ;
    cargo = Item.loadFrom(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveObject(followed) ;
    s.saveObject(tracked) ;
    s.saveObject(passenger) ;
    Item.saveTo(s, cargo) ;
  }
  
  
  
  /**  Performing regular updates-
    */
  public void updateAsScheduled(int numUpdates) {
  }
  
  
  protected void updateAsMobile() {
    //
    //  Firstly, check whether you even need to exist any more-
    super.updateAsMobile() ;
    if (
      (! followed.inWorld()) ||
      ! followed.mind.agenda().includes(tracked)
    ) {
      if (passenger != null) {
        ////I.say("Depositing passenger...") ;
        final Tile o = origin() ;
        passenger.setPosition(o.x, o.y, world) ;
      }
      exitWorld() ;
      return ;
    }
    //
    //  If so, update your position so as to follow behind the actor-
    final Vec3D FP = followed.position(null) ;
    final Vec2D FR = new Vec2D().setFromAngle(followed.rotation()) ;
    final float idealDist = followed.radius() + this.radius() ;
    FR.scale(0 - idealDist) ;
    FP.x += FR.x ;
    FP.y += FR.y ;
    nextPosition.setTo(FP) ;
    nextPosition.z = aboveGroundHeight() ;
    nextRotation = followed.rotation() ;
    //
    //  And if you have a passenger, update their position.
    if (passenger != null) {
      if (followed.indoors()) {
        final Boardable toBoard = followed.aboard() ;
        ///I.say("Transferring "+passenger+" to: "+toBoard) ;
        passenger.goAboard(toBoard, world) ;
        passenger.setHeading(toBoard.position(null), 0, false, world) ;
      }
      else {
        final Vec3D raise = new Vec3D(nextPosition) ;
        raise.z += 0.15f ;
        passenger.setHeading(raise, nextRotation, false, world) ;
      }
    }
  }
  
  
  protected float aboveGroundHeight() { return 0.15f ; }
  public float radius() { return 0.0f ; }
  public Base base() { return null ; }
  
  
  
  /**  Rendering and interface methods-
    */
  public void renderFor(Rendering rendering, Base base) {
    if (followed.indoors()) return ;
    if (origin().owner() != null) return ;
    super.renderFor(rendering, base) ;
  }
  
  
  public void describeStatus(Description d) {
    if (passenger != null) {
      d.append("Carrying ") ;
      d.append(passenger) ;
    }
    else if (cargo != null) {
      d.append("Carrying "+cargo) ;
    }
    else d.append("Idling") ;
  }
}








