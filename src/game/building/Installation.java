

package src.game.building ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.user.* ;



/**  This class is intended specifically to work with the InstallTab class to
  *  enable placement of irregularly-shaped fixtures and venues.
  */
public interface Installation {
  
  
  boolean pointsOkay(Tile from, Tile to) ;
  void doPlace(Tile from, Tile to) ;
  void preview(boolean canPlace, Rendering rendering, Tile from, Tile to) ;
  
  String fullName() ;
  Composite portrait(BaseUI UI) ;
  String helpInfo() ;
  String buildCategory() ;
}















