/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */
package src.util ;


public interface Series <T> extends Iterable <T> {
  
  int size() ;
  void add(T t) ;
  Object[] toArray() ;
  Object[] toArray(Class typeClass) ;
}
