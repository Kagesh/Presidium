/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.common ;
import src.game.common.WorldSections.Section ;
import src.util.* ;
import src.game.building.* ;
import java.util.Iterator ;



public class PresenceMap implements Session.Saveable {  //Do not make Saveable.
  
  
  
  /**  Fields, constructors, and save/load methods-
    */
  final Object key ;  //  Move this stuff to the Presences class.
  final World world ;
  final Node root ;
  
  static class Node extends List {
    final Section section ;
    int population = 0 ;
    Node(Section s) { this.section = s ; }
  }
  
  private Vec3D temp = new Vec3D() ;
  
  
  
  public PresenceMap(World world, Object key) {
    this.world = world ;
    this.root = new Node(world.sections.root) ;
    //
    //  Check to ensure you've been given a valid key-
    boolean keyOkay = false ;
    if (key instanceof String) keyOkay = true ;
    if (key instanceof Class) keyOkay = true ;
    if (key instanceof Session.Saveable) keyOkay = true ;
    if (key instanceof Service) keyOkay = true ;
    if (! keyOkay) I.complain("INVALID FLAGGING KEY: "+key) ;
    this.key = key ;
  }
  
  
  public PresenceMap(Session s) throws Exception {
    s.cacheInstance(this) ;
    world = s.world() ;
    
    final int keyType = s.loadInt() ;
    if (keyType == 0) key = s.loadClass() ;
    else if (keyType == 1) {
      key = s.loadString() ;
      ///I.say("Loading presence map with key: "+key) ;
    }
    else key = s.loadObject() ;
    //
    //  Load the root node from disk-
    root = new Node(world.sections.root) ;
    final int numLoad = s.loadInt() ;
    for (int n = numLoad ; n-- > 0 ;) loadMember(s) ;
  }
  
  
  public void saveState(Session s) throws Exception {
    if (key instanceof Class) {
      s.saveInt(0) ;
      s.saveClass((Class) key) ;
    }
    if (key instanceof String) {
      s.saveInt(1) ;
      s.saveString((String) key) ;
      ///I.say("Saving presence map with key: "+key) ;
    }
    if (key instanceof Session.Saveable) {
      s.saveInt(2) ;
      s.saveObject((Session.Saveable) key) ;
    }
    //
    //  Save the root node to disk-
    s.saveInt(root.population) ;
    saveNode(root, s) ;
  }
  
  
  private void loadMember(Session s) throws Exception {
    final int pX = s.loadInt(), pY = s.loadInt() ;
    final Target t = s.loadTarget() ;
    if (! t.inWorld()) I.say(t+" NOT IN WORLD! "+this.key) ;
    else toggleAt(root, pX, pY, t, true) ;
  }
  
  
  private void saveNode(Node n, Session s) throws Exception {
    final boolean leaf = n.section.depth == 0 ;
    for (Object k : n) if (k != null) {
      if (leaf) {
        final Target t = (Target) k ;
        t.position(temp) ;
        s.saveInt((int) temp.x) ;
        s.saveInt((int) temp.y) ;
        s.saveTarget(t) ;
      }
      else saveNode((Node) k, s) ;
    }
  }
  
  
  public int population() {
    return root.population ;
  }
  
  
  
  /**  Inserting and deleting members-
    */
  public void toggleMember(Target t, boolean is) {
    t.position(temp) ;
    toggleAt(root, (int) temp.x, (int) temp.y, t, is) ;
  }
  
  //
  //  NOTE:  This method should ONLY be used if you are very confident that the
  //  target in question either is or immediately WILL be at the given tile.
  public void toggleMember(Target t, Tile at, boolean is) {
    if (at == null) return ;
    toggleAt(root, at.x, at.y, t, is) ;
  }
  
  
  private void toggleAt(Node n, int x, int y, Target t, boolean is) {
    if (n.section.depth == 0) {
      final int oldPop = n.size() ;
      if (is) n.include(t) ;
      else n.remove(t) ;
      n.population += n.size() - oldPop ;
    }
    else {
      Section worldKid = null ; for (Section k : n.section.kids) {
        if (k.area.contains(x, y)) { worldKid = k ; break ; }
      }
      Node nodeKid = null ; for (Object o : n) {
        final Node k = (Node) o ;
        if (k.section == worldKid) { nodeKid = k ; break ; }
      }
      if (nodeKid == null) n.add(nodeKid = new Node(worldKid)) ;
      toggleAt(nodeKid, x, y, t, is) ;
      if (nodeKid.size() == 0) n.remove(nodeKid) ;
      
      n.population = 0 ;
      for (Object k : n) n.population += ((Node) k).population ;
    }
  }
  
  
  
  /**  Iterating through members-
    */
  public Iterable <Target> visitNear(
    final Target origin, final float range, final Box2D area
  ) {
    origin.position(temp) ;
    final int oX = (int) temp.x, oY = (int) temp.y ;
    //
    //  Firstly, we define the data structures needed to traverse the tree-
    //  structure of our entries and sort them by distance.
    final class NodeEntry {
      
      final Object node ;
      final float distance ;
      
      NodeEntry(final Object n) {
        this.node = n ;
        this.distance = (n instanceof Node) ?
          ((Node) n).section.area.distance(oX, oY) :
          Spacing.distance(origin, (Target) n) ;
      }
    }
    final Sorting <NodeEntry> agenda = new Sorting <NodeEntry> () {
      public int compare(NodeEntry a, NodeEntry b) {
        if (a.node == b.node) return 0 ;
        return a.distance < b.distance ? 1 : -1 ;
      }
      /*
      protected boolean greater(NodeEntry a, NodeEntry b) {
        return a.distance < b.distance ;
      }
      protected boolean match(NodeEntry a, NodeEntry b) {
        return a.node == b.node ;
      }
      //*/
    } ;
    agenda.insert(new NodeEntry(root)) ;
    //
    //  Then, define a method of iterating over these entries, and return it-
    final class nearIter implements Iterator, Iterable {
      
      private Object next = nextTarget() ;
      
      private Object nextTarget() {
        while (agenda.size() > 0) {
          //
          //  We obtain the next entry in the agenda-
          final Object ref = agenda.greatestRef() ;
          final NodeEntry entry = agenda.refValue(ref) ;
          final Object nearest = entry.node ;
          agenda.deleteRef(ref) ;
          //
          //  If it's not a node, return this.  Otherwise, add the children of
          //  the node to the agenda.  Reject anything out of range.
          if (range > 0 && entry.distance > range) continue ;
          if (nearest instanceof Node) {
            if (area != null) {
              final Box2D b = ((Node) nearest).section.area ;
              if (! area.intersects(b)) continue ;
            }
          }
          else {
            if (area != null) {
              ((Target) nearest).position(temp) ;
              if (! area.contains(temp.x, temp.y)) continue ;
            }
            return nearest ;
          }
          final Node node = (Node) nearest ;
          for (Object o : node) if (o != null) {
            agenda.insert(new NodeEntry(o)) ;
          }
        }
        return null ;
      }
      
      public boolean hasNext() {
        return next != null ;
      }

      public Object next() {
        final Object target = next ;
        next = nextTarget() ;
        return target ;
      }

      public void remove() {}
      public Iterator iterator() { return this ; }
    }
    return new nearIter() ;
  }
  
  
  public Target pickNearest(Target origin, float range) {
    for (Target t : visitNear(origin, range, null)) return t ;
    return null ;
  }
  

  public Target pickRandomAround(final Target origin, final float range) {
    origin.position(temp) ;
    final int oX = (int) temp.x, oY = (int) temp.y ;
    Node node = root ;
    while (true) {
      //
      //  For a given node level, iterate across all children and calculate the
      //  probability of visiting those.
      final Section quadKids[] = node.section.kids ;
      final boolean leaf = node.section.depth == 0 ;
      float weights[] = new float[node.size()], sumWeights = 0 ;
      int i = 0 ; for (Object k : node) {
        final float dist, pop ;
        if (leaf) {
          dist = Spacing.distance((Target) k, origin) ;
          pop = 1 ;
        }
        else {
          dist = ((Node) k).section.area.distance(oX, oY) ;
          pop = ((Node) k).population ;
        }
        if (range > 0 && dist > range) continue ;
        sumWeights += weights[i++] =
          pop * World.SECTION_RESOLUTION / (World.SECTION_RESOLUTION + dist) ;
      }
      //
      //  If no child is a valid selection, quit.  Otherwise, choose one child
      //  at random.
      if (sumWeights == 0) return null ;
      final float roll = Rand.num() * sumWeights ;
      int kidIndex = -1 ; sumWeights = 0 ;
      do { sumWeights += weights[++kidIndex] ; } while (sumWeights < roll) ;
      //
      //  If it's a leaf, return it.  Otherwise, move down the next level.
      if (leaf) return (Target) node.getEntryAt(kidIndex).refers ;
      node = (Node) node.getEntryAt(kidIndex).refers ;
    }
  }
}













