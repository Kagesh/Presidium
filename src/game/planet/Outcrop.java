


package src.game.planet ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.user.* ;
import src.util.* ;



public class Outcrop extends Fixture {
  
  
  /**  These are utility methods intended to determine the type and appearance
    *  of an outcrop based on underlying terrain type and mineral content.
    */
  //
  //  TODO:  In a later version, you might want to have different outcrop types
  //  for different forms of terrain...
  final public static int
    TYPE_MESA    =  0,
    TYPE_DUNE    =  1,
    TYPE_DEPOSIT =  2 ;
  
  
  static float rubbleFor(Outcrop outcrop, World world) {
    float rubble = 0, sum = 0 ; ;
    for (Tile t : outcrop.surrounds()) if (t != null) {
      rubble += t.habitat().rockiness ;
      sum++ ;
    }
    return rubble * 0.1f / sum ;
  }
  
  
  static int mineralTypeFor(Outcrop outcrop, World world) {
    //
    //  First, we sum up the total for each mineral type in the surrounding
    //  area (including the rockiness of the terrain.)
    float amounts[] = new float[4] ;
    int numTiles = 0 ;
    for (Tile t : outcrop.surrounds()) if (t != null) {
      final byte type = world.terrain().mineralType(t) ;
      final float amount = world.terrain().mineralsAt(t, type) ;
      amounts[type] += amount ;
      amounts[0] += t.habitat().rockiness ;
      numTiles++ ;
    }
    amounts[0] *= Rand.num() / 2f ;
    //
    //  Then perform a weighted pick from the range of types (having tweaked
    //  the odds a little...)
    float sumAmounts = 0 ;
    for (int i = 4 ; i-- > 0 ;) {
      final float a = amounts[i] ;
      sumAmounts += (amounts[i] = (a + (a * a)) / 2) ;
    }
    float pickRoll = Rand.num() * sumAmounts ;
    int pickType = 0 ;
    int type = 0 ; for (float f : amounts) {
      if (pickRoll < f) { pickType = type ; break ; }
      pickRoll -= f ;
      type++ ;
    }
    return pickType ;
  }
  
  
  static Model modelFor(Outcrop outcrop, World world) {
    
    final int mineral = mineralTypeFor(outcrop, world) ;
    final float rubble = rubbleFor(outcrop, world) ;
    outcrop.mineral = mineral ;
    final int size = outcrop.size, type = outcrop.type ;
    
    if (size == 1 && type != TYPE_DUNE) {
      return Habitat.SPIRE_MODELS[Rand.index(3)][2] ;
    }
    if (type == TYPE_DUNE) {
      return Habitat.DUNE_MODELS[Rand.index(3)] ;
    }
    if (mineral == 0 || size != 3) {
      int highID = Rand.yes() ? 1 : (3 - size) ;
      //int highID = (size == 3) ? 0 : 1 ;
      return Habitat.SPIRE_MODELS[Rand.index(3)][highID] ;
    }
    else {
      return Rand.num() < rubble ?
        Habitat.ROCK_LODE_MODELS[mineral - 1] :
        Habitat.MINERAL_MODELS[mineral - 1] ;
    }
  }
  
  
  
  /**  Data fields, constructors and save/load methods-
    */
  final int type ;
  int mineral = -1 ;
  
  
  public Outcrop(int size, int high, int type) {
    super(size, high * size) ;
    this.type = type ;
  }
  
  
  public Outcrop(Session s) throws Exception {
    super(s) ;
    type = s.loadInt() ;
    mineral = s.loadInt() ;
    if (size > 1 || type == TYPE_DUNE) sprite().scale = size / 2f ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(type) ;
    s.saveInt(mineral) ;
  }
  

  public boolean canPlace() {
    //  This only gets called just before entering thw rodl, so I think I can
    //  put this here.  TODO:  Move the location-verification code from the
    //  TerrainGen class to here?  ...Might be neater.
    final World world = origin().world ;
    for (Tile t : world.tilesIn(area(), false)) {
      if (t == null || t.blocked()) return false ;
    }
    return true ;
  }
  
  
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
    final Model model = modelFor(this, world) ;
    final Sprite s = model.makeSprite() ;
    if (size > 1 || type == TYPE_DUNE) s.scale = size / 2f ;
    attachSprite(s) ;
    setAsEstablished(true) ;
  }
  
  
  public int owningType() {
    return type == TYPE_DUNE ?
      Element.ELEMENT_OWNS :
      Element.TERRAIN_OWNS ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  public String fullName() {
    return "Outcrop" ;
  }
  
  
  public Texture portrait() {
    return null ;
  }
  
  
  public String helpInfo() {
    return
      "Rock outcrops are a frequent indication of underlying mineral wealth." ;
  }

  public String[] infoCategories() {
    return null ;
  }
  
  
  public void writeInformation(Description d, int categoryID) {
    d.append(helpInfo()) ;
  }
  
  
  public void whenClicked() {
  }
}








