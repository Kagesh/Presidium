/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.base ;
import src.game.common.* ;
import src.game.planet.Planet ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.util.* ;



public class Crop implements Session.Saveable, Target {
  
  
  final static int
    AS_SEED     = -1,
    NOT_PLANTED =  0,
    MIN_GROWTH  =  1,
    MIN_HARVEST =  3,
    MAX_GROWTH  =  4 ;
  final static String STAGE_NAMES[] = {
    "Seed Stock ",
    "Unplanted ",
    "Sprouting ",
    "Growing ",
    "Mature ",
    "Ripened "
  } ;
  final static String HEALTH_NAMES[] = {
    "Weak",
    "Poor",
    "Fair",
    "Good",
    "Excellent",
    "Perfect"
  } ;
  
  
  final Plantation parent ;
  final Tile tile ;
  
  int varID ;
  float growStage, health ;
  boolean infested ;
  
  
  public Crop(int varID) {
    this.parent = null ;
    this.tile = null ;
    this.varID = varID ;
    growStage = AS_SEED ;
  }
  
  
  protected Crop(Plantation parent, int varID, Tile t) {
    this.parent = parent ;
    this.varID = varID ;
    this.tile = t ;
    growStage = NOT_PLANTED ;
    health = 1.0f ;
  }
  
  
  public Crop(Session s) throws Exception {
    s.cacheInstance(this) ;
    parent = (Plantation) s.loadObject() ;
    tile = (Tile) s.loadTarget() ;
    varID = s.loadInt() ;
    growStage = s.loadFloat() ;
    health = s.loadFloat() ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveObject(parent) ;
    s.saveTarget(tile) ;
    s.saveInt(varID) ;
    s.saveFloat(growStage) ;
    s.saveFloat(health) ;
  }
  
  
  
  /**  Implementing the Target interface-
    */
  private Object flagged ;
  public boolean inWorld() { return parent.inWorld() ; }
  public boolean destroyed() { return parent.destroyed() ; }
  public Vec3D position(Vec3D v) { return tile.position(v) ; }
  public float height() { return tile.height() ; }
  public float radius() { return tile.radius() ; }
  public void flagWith(Object f) { this.flagged = f ; }
  public Object flaggedWith() { return flagged ; }
  
  
  
  /**  Updates and queries-
    */
  void doGrowth() {
    if (growStage == NOT_PLANTED) return ;
    final World world = parent.world() ;
    //
    //  Increment growth based on terrain fertility and daylight values.
    float growInc = tile.habitat().moisture() / 10f ;
    growInc += parent.belongs.growBonus(tile, varID, true) ;
    growInc *= Rand.num() * Planet.dayValue(world) ;
    growInc = Visit.clamp(growInc, 0.2f, 1.2f) / 2 ;
    if (infested) growInc /= 5 ;
    this.growStage = Visit.clamp(growStage + growInc, MIN_GROWTH, MAX_GROWTH) ;
    //
    //  Increase the chance of becoming infested based on pollution and
    //  proximity to other diseased plants of the same species, but reduce it
    //  based on intrinsic health rating and insect services.
    final int hive = Plantation.VAR_HIVE_CELLS ;
    final float pollution = world.ecology().squalorRating(tile) ;
    float infectChance = (((5 - health) / 10) + pollution) / 2f ;
    //
    //  (Hive cells can themselves become infested, but only from other hives.)
    for (Tile t : tile.allAdjacent(null)) {
      if (t == null || ! (t.owner() instanceof Plantation)) continue ;
      final Crop near = ((Plantation) t.owner()).plantedAt(t) ;
      if (near == null) continue ;
      if (near.varID == hive && this.varID != hive) {
        infectChance -= near.growStage / 10f ;
      }
      if (near.varID == this.varID) {
        infectChance += 0.1f / (near.infested ? 1 : 2) ;
      }
      else if (this.varID != hive) {
        infectChance += 0.1f / (near.infested ? 2 : 4) ;
      }
    }
    if (Rand.num() < infectChance) this.infested = true ;
    I.sayAbout(parent, "  Grown: "+this) ;
  }
  
  
  boolean needsTending() {
    return
      infested ||
      growStage == NOT_PLANTED ||
      growStage >= MIN_HARVEST ;
  }
  
  
  
  /**  Rendering and interface-
    */
  public String toString() {
    if (growStage == AS_SEED) return Plantation.CROP_NAMES[varID] ;
    int stage = (int) Visit.clamp(growStage + 1, 0, MIN_HARVEST + 1) ;
    final String HD ;
    if (infested) {
      HD = " (Infested)" ;
    }
    else {
      final int HL = Visit.clamp((int) health, 5) ;
      HD = " ("+HEALTH_NAMES[HL]+" health)" ;
    }
    return STAGE_NAMES[stage]+""+Plantation.CROP_NAMES[varID]+HD ;
  }
}







