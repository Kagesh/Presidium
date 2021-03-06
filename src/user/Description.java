/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */

package src.user ;
import src.graphics.widgets.* ;
import src.graphics.widgets.Text.Clickable;
import src.graphics.common.* ;
import src.util.* ;
import java.lang.reflect.* ;


public interface Description {
  
  
  public void append(String s, Clickable link, Colour c) ;
  public void append(Clickable link, Colour c) ;
  public void append(Clickable link) ;
  public void append(String s, Clickable link) ;
  public void append(String s, Colour c) ;
  public void append(String s) ;
  public void append(Object o) ;
  
  public void appendList(String s, Series l) ;
  public void appendList(String s, Object... l) ;
  
  public boolean insert(Image graphic, int maxSize) ;
  public boolean insert(Texture graphic, int maxSize) ;
  
  
  public abstract static class Link implements Clickable {
    
    final String name ;
    
    public Link(String name) { this.name = name ; }
    
    public String fullName() { return name ; }
  }
}














