/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.common ;
import src.game.building.* ;
import src.graphics.common.* ;
import src.user.* ;
import src.util.* ;



public abstract class Fixture extends Element {
  
  
  
  /**  Field definitions, constructors, and save/load methods-
    */
  final public int size, high ;
  final Box2D area = new Box2D() ;
  
  
  public Fixture(int size, int high) {
    this.size = size ;
    this.high = high ;
  }
  
  
  public Fixture(Session s) throws Exception {
    super(s) ;
    size = s.loadInt() ;
    high = s.loadInt() ;
    area.loadFrom(s.input()) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    super.saveState(s) ;
    s.saveInt(size) ;
    s.saveInt(high) ;
    area.saveTo(s.output()) ;
  }
  
  
  
  /**  Life cycle, ownership and positioning-
    */
  public boolean canPlace() {
    if (origin() == null) return false ;
    //final Box2D around = new Box2D().setTo(area()) ;//.expandBy(1) ;
    for (Tile t : origin().world.tilesIn(area, false)) {
      if (t == null || ! t.habitat().pathClear) return false ;
      if (t.owningType() >= this.owningType()) return false ;
    }
    return true ;
  }
  
  
  protected boolean canTouch(Element e) {
    return e.owningType() <= this.owningType() ;
  }
  
  
  public void clearSurrounds() {
    final Box2D around = new Box2D().setTo(area()).expandBy(1) ;
    final World world = origin().world ;
    for (Tile t : world.tilesIn(around, false)) if (t != null) {
      if (t.owner() != null && t.owningType() < this.owningType()) {
        t.owner().setAsDestroyed() ;
      }
    }
    Tile exit = null ;
    if (this instanceof Venue) exit = ((Venue) this).mainEntrance() ;
    else {
      final Tile perim[] = Spacing.perimeter(area(), world) ;
      for (Tile p : perim) if (! p.blocked()) { exit = p ; break ; }
    }
    if (exit == null) I.complain("No exit point from "+this) ;
    for (Tile t : world.tilesIn(area(), false)) {
      for (Mobile m : t.inside()) {
        m.setPosition(exit.x, exit.y, world) ;
      }
    }
  }
  
  
  public Tile[] surrounds() {
    final Box2D around = new Box2D().setTo(area()).expandBy(1) ;
    final World world = origin().world ;
    final Tile result[] = new Tile[(int) (around.xdim() * around.ydim())] ;
    int i = 0 ; for (Tile t : world.tilesIn(around, false)) {
      result[i++] = t ;
    }
    return result ;
  }
  
  
  public void enterWorldAt(int x, int y, World world) {
    super.enterWorldAt(x, y, world) ;
    for (Tile t : world.tilesIn(area, false)) {
      t.setOwner(this) ;
    }
  }
  
  
  public void setPosition(float x, float y, World world) {
    super.setPosition(x, y, world) ;
    final Tile o = origin() ;
    area.set(o.x - 0.5f, o.y - 0.5f, size, size) ;
  }
  
  
  public void exitWorld() {
    for (Tile t : world().tilesIn(area, false)) {
      t.setOwner(null) ;
    }
    super.exitWorld() ;
  }
  
  
  public int xdim() { return size ; }
  public int ydim() { return size ; }
  public int zdim() { return high ; }
  public float height() { return zdim() ; }
  public float radius() { return size / 2f ; }
  public Box2D area() { return area ; }
  
  
  public Box2D area(Box2D put) {
    if (put == null) put = new Box2D() ;
    return put.setTo(area) ;
  }
  
  
  public Vec3D position(Vec3D v) {
    final Tile o = origin() ;
    if (o == null) return null ;
    if (v == null) v = new Vec3D() ;
    v.set(
      o.x + (size / 2f) - 0.5f,
      o.y + (size / 2f) - 0.5f,
      o.elevation()
    ) ;
    return v ;
  }
  
  
  public int owningType() {
    return FIXTURE_OWNS ;
  }
  
  
  
  /**  Rendering and interface methods-
    */
  protected float fogFor(Base base) {
    float maxFog = Float.NEGATIVE_INFINITY ;
    final Tile o = origin() ;
    for (int x = o.x ; x < o.x + size ; x++)
      for (int y = o.y ; y < o.y + size ; y++) {
        final float fog = base.intelMap.fogAt(world.tileAt(x,  y)) ;
        if (fog > maxFog) maxFog = fog ;
      }
    return maxFog ;
  }
}













