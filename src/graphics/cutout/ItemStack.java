

package src.graphics.cutout ;
import src.graphics.common.* ;




/**  Provides a visual representation of a stack of items within the inventory
  *  of a particular venue.
  */
public class ItemStack extends GroupSprite {
  
  
  final public static Model
    CRATE_MODEL = ImageModel.asIsometricModel(
      ItemStack.class, "media/Items/crate.gif",
      0.5f, 0.2f
    ) ;

  final static float H = 0.25f, L = -0.25f, ATTACH_COORDS[][] = {
    {L, H}, {H, H}, {L, L}, {H, L}
  } ;
  final static int ITEM_UNIT = 5 ;
  
  
  final Model itemModel ;
  private int amount = 0 ;
  
  
  protected ItemStack(Model model) {
    this.itemModel = model ;
  }
  
  
  protected void updateAmount(int newAmount) {
    final int oldAmount = this.amount ;
    if (oldAmount == newAmount) return ;
    clearAllAttachments() ;
    //
    //  First, determine how many crates and packets of the good should be
    //  shown-
    int numPacks = (int) Math.ceil(newAmount * 1f / ITEM_UNIT), numCrates = 0 ;
    while (numPacks > 4) { numPacks -= 4 ; numCrates++ ; }
    final int
      total = numCrates + numPacks,
      numLevels = total / 4,
      topOffset = (4 * (int) Math.ceil(total / 4f)) - total ;
    //
    //  Then iterate through the list of possible positions, and fill 'em up.
    for (int i = 0 ; i < total ; i++) {
      final int level = i / 4, coordIndex = (level < numLevels) ?
        (i % 4) :
        (i % 4) + topOffset ;
      final float coord[] = ATTACH_COORDS[coordIndex] ;
      attach(
        (i < numCrates) ? CRATE_MODEL : itemModel,
        coord[0], coord[1], level * 0.2f
      ) ;
    }
    amount = newAmount ;
  }
}






