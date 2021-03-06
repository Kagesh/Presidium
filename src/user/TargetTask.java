


package src.user ;
import src.game.common.* ;
import src.graphics.common.* ;
import src.util.* ;



public abstract class TargetTask implements UITask {
  
  
  BaseUI UI ;
  Texture cursor ;

  //Target from, to ;
  //boolean valid ;
  
  
  protected TargetTask(BaseUI UI, Texture cursor) {
    this.UI = UI ;
    this.cursor = cursor ;
  }
  
  
  
  abstract boolean validPick(Target pick) ;
  abstract void previewAt(Target picked, boolean valid) ;
  abstract void performAt(Target picked) ;
  
  

  public void doTask() {
    //
    //  Get the currently picked tile, fixture and mobile.  See if they are
    //  valid as targets.  If so, preview in green.  Otherwise, preview in
    //  red.  If the user clicks, perform the placement.
    final Tile PT = UI.selection.pickedTile() ;
    final Fixture PF = UI.selection.pickedFixture() ;
    final Mobile PM = UI.selection.pickedMobile() ;
    
    boolean valid = true ;
    Target picked = null ;
    if      (validPick(PM)) picked = PM ;
    else if (validPick(PF)) picked = PF ;
    else if (validPick(PT)) picked = PT ;
    else { picked = PT ; valid = false ; }
    
    if (picked != null) {
      previewAt(picked, valid) ;
      if (UI.mouseClicked() && valid) {
        performAt(picked) ;
        UI.endCurrentTask() ;
      }
    }
  }
  
  
  
  public void cancelTask() {
    UI.endCurrentTask() ;
  }
  
  
  public Texture cursorImage() {
    return cursor ;
  }
}







