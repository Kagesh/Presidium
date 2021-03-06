/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.game.common ;
import java.io.* ;
import java.lang.reflect.* ;
import src.util.* ;
import src.graphics.common.Model ;


//
//  TODO:  It would be very helpful to include some supplementary information
//  here along with the save file listing how many bytes of data each save-ID-
//  tagged object should be reading in after writing out.  That would help to
//  nail down any discrepancies.


/**  NOTE:  Saveable objects ALSO need to implement a public constructor that
  *  takes a Session as it's sole argument, or an exception will occur, AND
  *  the object must call cacheInstance() as soon as possible once initialised,
  *  or an exception will occur.  Alternatively, they may implement a static
  *  loadConstant method taking the Session as it's argument.
  *  
  *  The Saveable interface is accessible from within the Session class.
  */
public class Session {
  
  
  private PlayLoop playLoop = null ;
  private World world = null ;
  
  
  
  final static int
    CLASS_CAPACITY  = 200,
    OBJECT_CAPACITY = 50000 ;
  
  private Table <Class, Vars.Int> classCounts = new Table(CLASS_CAPACITY) ;
  
  final Table               < Saveable, Integer>
    saveIDs  = new Table    < Saveable, Integer> (OBJECT_CAPACITY) ;
  final Table               < Class <Object>, Integer >
    classIDs = new Table    < Class <Object>, Integer > (CLASS_CAPACITY ) ;
  final Table               < Integer, Saveable>
    loadIDs  = new Table    < Integer, Saveable> (OBJECT_CAPACITY) ;
  final Table               < Integer, Object>
    loadMethods = new Table < Integer, Object> (CLASS_CAPACITY ) ;
  private int
    nextObjectID = 0,
    nextClassID  = 0,
    lastObjectID = -1 ;
  
  private boolean saving ;
  private DataOutputStream out ;
  private DataInputStream  in  ;
  private int bytesIn = 0, bytesOut = 0 ;
  
  
  /**  Methods for saving and loading session data:
    */
  public static Session saveSession(
    World world, PlayLoop loop, String saveFile
  ) throws Exception {
    final Session s = new Session() ;
    s.out = new DataOutputStream(new BufferedOutputStream(
      new FileOutputStream(saveFile))
    ) ;
    s.saving = true ;
    Model.clearModelIDs() ;
    s.world = world ;
    s.saveInt(world.size) ;
    s.world.saveState(s) ;
    s.playLoop = loop ;
    s.saveObject(s.playLoop) ;
    s.finish() ;
    
    I.say("\nDISPLAYING TOTAL SAVE COUNTS:") ;
    for (Class CC : s.classCounts.keySet()) {
      final Vars.Int count = s.classCounts.get(CC) ;
      I.say("  Saved "+count.val+" of "+CC.getSimpleName()) ;
    }
    
    return s ;
  }
  
  
  public static Session loadSession(String saveFile) throws Exception {
    final Session s = new Session() ;
    s.in = new DataInputStream(new BufferedInputStream(
      new FileInputStream(saveFile))
    ) ;
    s.saving = false ;
    Model.clearModelIDs() ;
    s.world = new World(s.loadInt()) ;
    s.world.loadState(s) ;
    s.playLoop = (PlayLoop) s.loadObject() ;
    s.finish() ;
    return s ;
  }
  
  
  private Session() {}
  
  public void finish() throws Exception {
    ///I.say("FINISHING SESSION.") ;
    world = null ;
    saveIDs.clear() ;
    classIDs.clear() ;
    loadIDs.clear() ;
    loadMethods.clear() ;
    if (out != null) {
      out.flush() ;
      out.close() ;
    }
    if (in != null) {
      in.close() ;
    }
  }
  
  public World world() { return world ; }
  public PlayLoop loop() { return playLoop ; }
  
  
  
  /**  NOTE:  Classes that implement this interface must ALSO implement a
    *  public constructor taking a Session as it's sole argument, with a
    *  cacheInstance call made right away, OR, a static loadConstant method
    *  taking the Session as it's argument.
    */
  public static interface Saveable {
    void saveState(Session s) throws Exception ;
  }
  
  
  /**  Saving and Loading series of objects-
    */
  public void saveObjects(Series objects) throws Exception {
    if (objects == null) {
      saveInt(-1) ;
      return ;
    }
    saveInt(objects.size()) ;
    for (Object o : objects) saveObject((Saveable) o) ;
  }
  
  
  public Series loadObjects(Series objects) throws Exception {
    final int count = loadInt() ;
    if (count == -1) return null ;
    for (int n = count ; n-- > 0 ;) objects.add(loadObject()) ;
    return objects ;
  }
  
  
  
  /**  Saving and loading of targets-
    */
  public void saveTarget(Target target) throws Exception {
    if (target == null) {
      saveInt(-1) ;
    }
    else if (target instanceof Tile) {
      saveInt(0) ;
      final Tile t = (Tile) target ;
      saveInt(t.x) ; saveInt(t.y) ;
    }
    else {
      saveInt(1) ;
      saveObject((Session.Saveable) target) ;
    }
  }
  
  
  public Target loadTarget() throws Exception {
    final int type = loadInt() ;
    if (type == -1) return null ;
    if (type == 0) return world.tileAt(loadInt(), loadInt()) ;
    return (Target) loadObject() ;
  }
  
  
  public void saveTargets(Series <Target> targets) throws Exception {
    if (targets == null) {
      saveInt(-1) ;
      return ;
    }
    saveInt(targets.size()) ;
    for (Target o : targets) saveTarget(o) ;
  }
  
  
  public Series <Target> loadTargets(Series <Target> targets) throws Exception {
    final int count = loadInt() ;
    if (count == -1) return null ;
    for (int n = count ; n-- > 0 ;) targets.add(loadTarget()) ;
    return targets ;
  }
  
  
  public void saveTargetArray(Target[] objects) throws Exception {
    if (objects == null) { saveInt(-1) ; return ; }
    saveInt(objects.length) ;
    for (Target o : objects) saveTarget(o) ;
  }
  
  
  public Target[] loadTargetArray(Class typeClass) throws Exception {
    final int num = loadInt() ;
    if (num == -1) return null ;
    final Target[] array = (Target[]) Array.newInstance(typeClass, num) ;
    for (int n = 0 ; n < num ; n++) array[n] = loadTarget() ;
    return array ;
  }
  
  
  
  /**  Saving and Loading of classes themselves-
    */
  public void saveClass(Class c) throws Exception {
    if (c == null) { out.writeInt(-1) ; return ; }
    final Integer classID = classIDs.get(c) ;
    if (classID == null) {
      //
      //  Then we need to save the full binary name of this class and cache
      //  it's ID-
      //I.say("Saving new class- "+c.getName()+" ID: "+nextClassID) ;
      out.writeInt(nextClassID) ;
      LoadService.writeString(out, c.getName()) ;
      classIDs.put(c, nextClassID++) ;
    }
    else out.writeInt(classID) ;
  }
  
  
  public Class loadClass() throws Exception {
    final int classID = in.readInt() ;
    if (classID == -1) return null ;
    final Object loadMethod = loadMethod(classID) ;
    if (loadMethod instanceof Constructor)
      return ((Constructor) loadMethod).getDeclaringClass() ;
    else
      return ((Method) loadMethod).getDeclaringClass() ;
  }
  
  
  
  /**  Caches the initialisation method (static or constructor) used to create
    *  new instances of a particular object class.
    */
  private Object loadMethod(final int classID) throws Exception {
    Object loadMethod = loadMethods.get(classID) ;
    if (loadMethod == null) {
      final String className = LoadService.readString(in) ;
      //I.say("Loading new class- "+className+" ID: "+classID) ;
      final ClassLoader loader = ClassLoader.getSystemClassLoader() ; 
      final Class loadClass = loader.loadClass(className) ;
      try {
        loadMethod = loadClass.getConstructor(Session.class) ;
      }
      catch (NoSuchMethodException e) {
        loadMethod = loadClass.getMethod("loadConstant", Session.class) ;
      }
      loadMethods.put(classID, loadMethod) ;
    }
    return loadMethod ;
  }
  
  
  public void saveObject(Saveable s) throws Exception {
    if (s == null) { out.writeInt(-1) ; return ; }
    final Integer saveID = saveIDs.get(s) ;
    if (saveID == null) {
      Vars.Int count = classCounts.get(s.getClass()) ;
      if (count == null) classCounts.put(s.getClass(), count = new Vars.Int()) ;
      count.val++ ;
      
      //I.say("Saving new object, class- "+s.getClass().getName()) ;
      saveIDs.put(s, nextObjectID) ;
      out.writeInt(nextObjectID++) ;
      saveClass(s.getClass()) ;
      final int initBytes = bytesOut ;
      s.saveState(this) ;
      /*
      I.say(
        "Saved new object: "+s.getClass().getName()+
        " total bytes saved: "+(bytesOut - initBytes)
      ) ;
      //*/
      bytesOut = initBytes ;
    }
    else {
      out.writeInt(saveID) ;
    }
  }
  
  
  
  /**  This method is intended to help avoid self-referential loop conditions by
    *  being called by setupDone objects IMMEDIATELY after being initialised and
    *  BEFORE any member fields or variables have been loaded.  (NOTE:  This
    *  does not apply to objects created by a static loadConstant() method...)
    */
  public void cacheInstance(Saveable s) {
    loadIDs.put(lastObjectID, s) ;
  }
  //
  //  This object exists for a similar reason-
  final static Saveable MARK_LOCK = new Saveable() {
    public void saveState(Session s) throws Exception {}
    
  } ;
  
  
  public Saveable loadObject() throws Exception {
    //I.say("Loading object...") ;
    final int loadID = in.readInt() ;
    if (loadID == -1) return null ;
    //I.say("Loading object of ID: "+loadID) ;
    
    Saveable loaded = loadIDs.get(loadID) ;
    if (loaded != null) {
      //I.say("Loading existing object: "+loaded) ;
      if (loaded == MARK_LOCK) {
        //  Hopefully this can't happen now...
        I.complain(
          "LOADING HAS HIT A SELF-REFERENTIAL LOOP CONDITION..."
        ) ;
      }
      else {
        return loaded ;
      }
    }
    //
    //  We use the MARK_LOCK Object as a placeholder to check if a given
    //  Saveable is being referred to before it can be cached, indicating a
    //  self-referential loop condition.
    loadIDs.put(lastObjectID = loadID, MARK_LOCK) ;
    final Object loadMethod = loadMethod(loadInt()) ;
    Class loadClass = null ;
    final int initBytes = bytesIn ;
    try {
      if (loadMethod instanceof Constructor) {
        final Constructor loadObject = (Constructor) loadMethod ;
        loadClass = loadObject.getDeclaringClass() ;
        //I.say("Loading new object of type "+loadClass.getName()) ;
        loaded = (Saveable) loadObject.newInstance(this) ;
      }
      else {
        final Method loadConstant = (Method) loadMethod ;
        loadClass = loadConstant.getDeclaringClass() ;
        //I.say("Loading new constant of type "+loadClass.getName()) ;
        loaded = (Saveable) loadConstant.invoke(null, this) ;
        cacheInstance(loaded) ;
      }
    }
    catch (InstantiationException e) { I.complain(
      "PROBLEM WITH "+loadClass.getName()+"\n"+
      "ALL CLASSES IMPLEMENTING SAVEABLE MUST IMPLEMENT A PUBLIC CONSTRUCTOR "+
      "TAKING THE SESSION AS IT'S SOLE ARGUMENT, OR A STATIC loadConstant("+
      "Session s) METHOD THAT RETURNS A SAVEABLE OBJECT. THANK YOU."
    ) ; }
    final Saveable cached = loadIDs.get(loadID) ;
    if (cached != loaded) I.complain(
      "PROBLEM WITH "+loadClass.getName()+"\n"+
      "ALL CLASSES IMPLEMENTING SAVEABLE SHOULD CACHE THEMSELVES USING THE "+
      "Session.cacheInstance(Saveable s) METHOD *IMMEDIATELY* AFTER BEING "+
      "INSTANCED- I.E, FIRST THING IN THE ROOT CONSTRUCTOR, BEFORE ANY "+
      "MEMBER FIELDS OR VARIABLES HAVE BEEN LOADED.  (THIS DOES NOT APPLY TO "+
      "THOSE THAT IMPLEMENT loadConstant(Session s).)  THANK YOU."
    ) ;
    bytesIn = initBytes ;
    return loaded ;
  }
  
  
  
  /**  These methods allow Saveable objects to import/export their internal
    *  data, and permit direct access to the data input/output streams if
    *  required.
    */
  public boolean saving() { return saving ; }
  
  
  public DataOutputStream output() { return out ; }
  public DataInputStream  input()  { return in ; }
  
  
  public int bytesIn() { return bytesIn ; }
  public int bytesOut() { return bytesOut ; }
  
  
  public void loadByteArray(byte array[][]) throws Exception {
    for (byte a[] : array) in.read(a) ;
  }
  
  
  public void saveByteArray(byte array[][]) throws Exception {
    for (byte a[] : array) out.write(a) ;
  }
  
  
  public float loadFloat() throws Exception {
    bytesIn += 4 ;
    return in.readFloat() ;
  }
  
  
  public void saveFloat(float f) throws Exception {
    out.writeFloat(f) ;
    bytesOut += 4 ;
  }
  
  
  public int loadInt() throws Exception {
    bytesIn += 4 ;
    return in.readInt() ;
  }
  

  public void saveInt(int i) throws Exception {
    out.writeInt(i) ;
    bytesOut += 4 ;
  }
  
  
  public boolean loadBool() throws Exception {
    bytesIn += 1 ;
    return in.readBoolean() ;
  }
  
  
  public void saveBool(boolean b) throws Exception {
    out.writeBoolean(b) ;
    bytesOut += 1 ;
  }
  
  
  public String loadString() throws Exception {
    final int len = in.readInt() ;
    if (len == -1) return null ;
    final byte chars[] = new byte[len] ;
    in.read(chars) ;
    bytesIn += len + 4 ;
    return new String(chars) ;
  }
  
  
  public void saveString(String s) throws Exception {
    if (s == null) { out.writeInt(-1) ; return ; }
    final byte chars[] = s.getBytes() ;
    out.writeInt(chars.length) ;
    out.write(chars) ;
    bytesOut += chars.length + 4 ;
  }
}