/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.common.* ;
import src.util.* ;



public abstract class TileSpread extends Search <Tile> {
  
  final Tile batch[] = new Tile[4] ;
  
  
  public TileSpread(Tile init) {
    super(init, -1) ;
  }
  
  
  protected abstract boolean canAccess(Tile t) ;
  protected abstract boolean canPlaceAt(Tile t) ;
  
  
  protected boolean canEnter(Tile spot) {
    return canAccess(spot) ;
  }
  
  protected Tile[] adjacent(Tile spot) {
    return spot.edgeAdjacent(batch) ;
  }
  
  protected boolean endSearch(Tile best) {
    return canPlaceAt(best) ;
  }
  
  protected float cost(Tile prior, Tile spot) {
    return 1 ;
  }
  
  protected float estimate(Tile spot) {
    return 0 ;
  }
  
  protected void setEntry(Tile spot, Entry flag) {
    spot.flagWith(flag) ;
  }
  
  protected Entry entryFor(Tile spot) {
    return (Entry) spot.flaggedWith() ;
  }
}