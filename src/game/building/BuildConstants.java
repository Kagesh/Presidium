/**  
  *  Written by Morgan Allen.
  *  I intend to slap on some kind of open-source license here in a while, but
  *  for now, feel free to poke around for non-commercial purposes.
  */


package src.game.building ;
import src.game.actors.ActorConstants ;
import src.game.base.* ;



/*
SPYCE_A = new Item.Type(C, COMMODITY, "Spyce A (Tinerazine)", 400),
SPYCE_B = new Item.Type(C, COMMODITY, "Spyce B (Halebdynum)", 400),
SPYCE_C = new Item.Type(C, COMMODITY, "Spyce C (Natrizoral)", 400),
//*/
  /*
  OUTFITS  = new Item.Type(C, UNIQUE, "Outfits", 0),
  DEVICES  = new Item.Type(C, UNIQUE, "Devices", 0),
  ARMOURS  = new Item.Type(C, UNIQUE, "Armours", 0),
  
  GENE_SAMPLE = new Item.Type(C, UNIQUE, "Gene Sample", 50 ),
  NEURAL_SCAN = new Item.Type(C, UNIQUE, "Neural Scan", 500),
  TROPHY      = new Item.Type(C, UNIQUE, "Trophy"     , 250)
  //*/



public interface BuildConstants extends ActorConstants {
  
  
  final public static int
    COMMODITY      = 0,
    PROVISION      = 1,
    UNIQUE         = 2,
    DEVICE         = 3,
    OUTFIT         = 4,
    SERVICE        = 5 ;
  
  final static Class BC = BuildConstants.class ;
  
  final public static Service
    
    STARCHES = new Service(BC, COMMODITY, "Starches", 10 ),
    PROTEIN  = new Service(BC, COMMODITY, "Protein" , 20 ),
    GREENS   = new Service(BC, COMMODITY, "Greens"  , 30 ),
    
    TIMBER   = new Service(BC, COMMODITY, "Timber"  , 40 ),
    STONE    = new Service(BC, COMMODITY, "Stone"   , 70 ),
    HIDES    = new Service(BC, COMMODITY, "Hides"   , 150),
    
    METALS   = new Service(BC, COMMODITY, "Metals"  , 15 ),
    CARBONS  = new Service(BC, COMMODITY, "Carbons" , 35 ),
    ISOTOPES = new Service(BC, COMMODITY, "Isotopes", 60 ),
    
    PARTS    = new Service(BC, COMMODITY, "Parts"   , 50 ),
    PLASTICS = new Service(BC, COMMODITY, "Plastics", 75 ),
    SOMA     = new Service(BC, COMMODITY, "Soma"    , 100),
    
    INSCRIPTION = new Service(BC, COMMODITY, "Inscription", 140),
    PRESSFEED   = new Service(BC, COMMODITY, "Pressfeed"  , 160),
    MEDICINE    = new Service(BC, COMMODITY, "Medicine"   , 200),
    
    CARRIED_ITEM_TYPES[] = Service.typesSoFar(),
    
    WATER        = new Service(BC, PROVISION, "Water"       , 0),
    LIFE_SUPPORT = new Service(BC, PROVISION, "Life Support", 0),
    POWER        = new Service(BC, PROVISION, "Power"       , 0),
    
    ALL_FOOD_TYPES[] = { STARCHES, PROTEIN, GREENS  },
    ALL_PROVISIONS[] = { WATER, LIFE_SUPPORT, POWER } ;

  final public static Service
    SERVICE_ADMIN   = new Service(BC, SERVICE, "Admin", 0),
    SERVICE_TREAT   = new Service(BC, SERVICE, "Treatment", 0),
    SERVICE_PERFORM = new Service(BC, SERVICE, "Performance", 0) ;
  
  
  final public static int
    NONE     = 0,
    //
    //  These are properties of equipped weapons-
    MELEE    = 1,
    RANGED   = 2,
    ENERGY   = 4,
    PHYSICAL = 8,
    STUN     = 16,
    POISON   = 32,
    //
    //  These are properties of natural weapons or armour-
    GRAPPLE      = 64,
    CAUSTIC      = 128,
    TRANSMORPHIC = 256,
    ENERGY_DRAIN = 512 ;
  
  //
  //  TODO:  These prices are far too low next to the value of the raw
  //  materials.  Also, you need medical devices and so on.
  final public static DeviceType
    MANIPLES = new DeviceType(BC, "Maniples",
      2, GRAPPLE | MELEE | PHYSICAL, 10,
      new Conversion(3, PARTS, 10, ASSEMBLY),
      "maniples"
    ),
    MODUS_LUTE = new DeviceType(BC, "Modus Lute",
      0, RANGED | STUN, 40,
      new Conversion(2, PARTS, 15, ASSEMBLY),
      "modus lute"
    ),
    CARVED_SPEAR = new DeviceType(BC, "Carved Spear",
      5, RANGED | PHYSICAL, 5,
      new Conversion(2, TIMBER, 5, ASSEMBLY),
      "spear"
    ),
    SHOCK_STAFF = new DeviceType(BC, "Shock Staff",
      10, MELEE | PHYSICAL | STUN, 40,
      new Conversion(3, PARTS, 10, ASSEMBLY),
      "staff"
    ),
    PHASE_PISTOL = new DeviceType(BC, "Phase Pistol",
      15, RANGED | ENERGY, 25,
      new Conversion(3, PARTS, 10, ASSEMBLY),
      "pistol"
    ),
    KONOCHE = new DeviceType(BC, "Konoche",
      20, MELEE | PHYSICAL, 45,
      new Conversion(2, PARTS, 5, ASSEMBLY),
      "heavy blade"
    ) ;
  final public static Service
    ALL_IMPLEMENTS[] = Service.typesSoFar() ;
  
  
  //
  //  TODO:  Should have skins associated with these?
  //  TODO:  Add 'Primitive Garb' and 'Overalls'?  What about costume?
  final public static OutfitType
    FINERY         = new OutfitType(
      BC, "Finery"        , 2 , 100,
      new Conversion(3, PLASTICS, Fabricator.class, 15, GRAPHIC_MEDIA)
    ),
    CAMOUFLAGE     = new OutfitType(
      BC, "Camouflage"    , 3 , 35,
      new Conversion(2, PLASTICS, Fabricator.class, 10, GRAPHIC_MEDIA)
    ),
    SEALSUIT       = new OutfitType(
      BC, "Sealsuit"      , 4 , 75,
      new Conversion(2, PLASTICS, 2, PARTS, Fabricator.class, 15, ASSEMBLY)
    ),
    SHIELD_BELT = new OutfitType(
      BC, "Shield Belt"   , 5 , 50,
      new Conversion(2, PARTS, Artificer.class, 5, ASSEMBLY)
    ),
    BODY_ARMOUR    = new OutfitType(
      BC, "Body Armour"   , 10, 75,
      new Conversion(5, PARTS, Artificer.class, 15, ASSEMBLY)
    ),
    GOLEM_ARMOUR   = new OutfitType(
      BC, "Golem Armour"  , 20, 150,
      new Conversion(12, PARTS, Artificer.class, 20, ASSEMBLY)
    ) ;
  final public static Service
    ALL_OUTFITS[] = Service.typesSoFar() ;
  
  final public static Service
    ALL_ITEM_TYPES[] = Service.allTypes() ;
  
  
  
  
  final public static Object TO = new Object() ;
  
  final public static Conversion
    
    /*
    TIMBER_TO_CARBONS = new Conversion(
      1, TIMBER, TO, 2, CARBONS,
      CuringShed.class, SIMPLE_DC, CHEMISTRY
    ),
    
    STARCHES_TO_CARBONS = new Conversion(
      1, STARCHES, TO, 1, CARBONS,
      CuringShed.class, SIMPLE_DC, CHEMISTRY
    ),
    
    ISOTOPES_TO_POWER = new Conversion(
      1, ISOTOPES, TO, 100, POWER,
      Reactor.class, DIFFICULT_DC, CHEMISTRY, ROUTINE_DC, FIELD_THEORY
    ),
    //*/
    
    METALS_TO_PARTS = new Conversion(
      2, METALS, TO, 1, PARTS,
      Artificer.class, TRICKY_DC, ASSEMBLY, SIMPLE_DC, CHEMISTRY
    ),
    
    CARBONS_TO_PLASTICS = new Conversion(
      2, CARBONS, TO, 1, PLASTICS,
      Fabricator.class, TRICKY_DC, CHEMISTRY, SIMPLE_DC, GRAPHIC_MEDIA
    ),
    
    NONE_TO_SOMA = new Conversion(
      TO, 1, SOMA,
      CultureVats.class, ROUTINE_DC, CHEMISTRY, ROUTINE_DC, PHARMACY
    ) ;
}















