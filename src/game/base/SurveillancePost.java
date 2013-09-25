

package src.game.base ;
import src.game.actors.* ;
import src.game.building.* ;
import src.game.common.* ;
import src.game.planet.* ;
import src.game.tactical.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.graphics.widgets.* ;
import src.user.* ;
import src.util.* ;



public class SurveillancePost extends Venue implements BuildConstants {
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final public static Model MODEL = ImageModel.asIsometricModel(
    SurveillancePost.class, "media/Buildings/ecologist/surveyor.png", 4, 1
  ) ;
  
  
  public SurveillancePost(Base base) {
    super(4, 1, Venue.ENTRANCE_EAST, base) ;
    structure.setupStats(
      100, 4, 150,
      Structure.SMALL_MAX_UPGRADES, Structure.TYPE_VENUE
    ) ;
    personnel.setShiftType(SHIFTS_BY_HOURS) ;
    attachSprite(MODEL.makeSprite()) ;
  }
  
  
  public SurveillancePost(Session s) throws Exception {
    super(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
  }
  
  
  
  /**  Upgrades, economic functions and behaviour implementations-
    */
  public Behaviour jobFor(Actor actor) {
    if ((! structure.intact()) || (! personnel.onShift(actor))) return null ;
    //
    //  Return a hunting expedition.   And... just explore the place.  You'll
    //  want to make this a bit more nuanced later.
    final Choice choice = new Choice(actor) ;
    final Actor p = Hunting.nextPreyFor(actor, World.DEFAULT_SECTOR_SIZE * 2) ;
    if (p != null) {
      final Hunting h = new Hunting(actor, p, Hunting.TYPE_HARVEST) ;
      //h.priorityMod = Plan.ROUTINE ;
      choice.add(h) ;
    }
    final Tile t = Exploring.getUnexplored(actor.base().intelMap, actor) ;
    if (t != null) {
      I.say("TILE FOUND IS: "+t) ;
      final Exploring e = new Exploring(actor, actor.base(), t) ;
      e.priorityMod = Plan.ROUTINE ;
      choice.add(e) ;
    }
    else I.say("NOTHING LEFT TO EXPLORE?") ;
    return choice.weightedPick(actor.AI.whimsy()) ;
  }
  
  
  protected Background[] careers() {
    return new Background[] { Background.EXPLORER } ;
  }
  
  
  public int numOpenings(Background v) {
    final int nO = super.numOpenings(v) ;
    if (v == Background.EXPLORER) return nO + 2 ;
    return 0 ;
  }
  
  
  public Service[] services() {
    return new Service[] { WATER, PROTEIN, SPICE } ;
  }
  
  
  
  /**  Rendering and interface-
    */
  public String fullName() {
    return "Surveillance Post" ;
  }
  
  
  public Composite portrait(HUD UI) {
    return new Composite(UI, "media/GUI/Buttons/redoubt_button.gif") ;
  }
  
  
  public String helpInfo() {
    return
      "Surveyors are responsible for exploring the hinterland of your "+
      "settlement, scouting for danger and regulating animal populations." ;
  }
  
  
  public String buildCategory() {
    return UIConstants.TYPE_ECOLOGIST ;
  }
}







