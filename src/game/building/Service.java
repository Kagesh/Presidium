


package src.game.building ;
import src.game.common.* ;
import src.game.actors.* ;
import src.graphics.common.* ;
import src.graphics.cutout.* ;
import src.util.* ;



/**  Used to represent the types of goods and services that venues can provide
  *  or produce.
  */
public class Service implements Session.Saveable {
  
  
  private static int nextID = 0 ;
  private static Batch allTypes = new Batch(), soFar = new Batch() ;
  
  final static String
    ITEM_PATH = "media/Items/" ;
  final static Texture
    DEFAULT_PIC = Texture.loadTexture(ITEM_PATH+"crate.gif") ;
  
  
  static Service[] typesSoFar() {
    Service t[] = (Service[]) soFar.toArray(Service.class) ;
    soFar.clear() ;
    return t ;
  }
  
  static Service[] allTypes() {
    return (Service[]) allTypes.toArray(Service.class) ;
  }
  
  
  final public int form ;
  final public String name ;
  final public int typeID = nextID++ ;
  
  final public int basePrice ;
  final public Texture pic ;
  final public Model model ;
  
  
  protected Service(
    Class typeClass, int form, String name,
    int basePrice
  ) {
    this(typeClass, name, null, form, basePrice) ;
  }
  
  
  protected Service(
    Class typeClass, String name, String imgName,
    int form, int basePrice
  ) {
    this.form = form ;
    this.name = name ;
    this.basePrice = basePrice ;
    final String imagePath = ITEM_PATH+imgName ;
    if (new java.io.File(imagePath).exists()) {
      this.pic = Texture.loadTexture(imagePath) ;
      this.model = ImageModel.asFlatModel(
        typeClass, Texture.loadTexture(imagePath), 0.5f
      ) ;
    }
    else {
      this.pic = DEFAULT_PIC ;
      this.model = ImageModel.asFlatModel(typeClass, DEFAULT_PIC, 0.5f) ;
    }
    soFar.add(this) ;
    allTypes.add(this) ;
  }
  
  
  public static Service loadConstant(Session s) throws Exception {
    return BuildConstants.ALL_ITEM_TYPES[s.loadInt()] ;
  }
  
  
  public void saveState(Session s) throws Exception {
    s.saveInt(typeID) ;
  }
  
  
  public Conversion materials() { return null ; }
  
  
  public static interface Trade extends Inventory.Owner {
    public float importShortage(Service type) ;
    public float exportSurplus(Service type) ;
    public float importDemand(Service type) ;
    public float exportDemand(Service type) ;
  }
  
  
  
  public String toString() { return name ; }
}









